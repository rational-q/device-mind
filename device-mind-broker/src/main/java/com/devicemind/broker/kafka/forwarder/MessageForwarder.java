package com.devicemind.broker.kafka.forwarder;

import com.devicemind.broker.service.MqttMessageStore;
import com.devicemind.common.kafka.model.DeviceResponseEvent;
import com.devicemind.common.kafka.producer.DeviceDataProducer;
import com.devicemind.common.kafka.producer.DeviceResponseProducer;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息转发服务 — 将 MQTT 消息路由到对应 Kafka topic
 * <p>
 * 路由规则：
 * <ul>
 *   <li>{@code device/response/*} → device-response（设备指令回执）</li>
 *   <li>其它（{@code device/data/*} 等） → device-data（设备上报数据）</li>
 * </ul>
 * <p>
 * 设备回执的原始 payload 是设备对指令的执行结果，约定结构与下发指令一致：
 * {@code {"command":"...","idempotencyKey":"...","params":{...}}}，
 * 这里解析后重新构造标准 {@link DeviceResponseEvent} 投递到 device-response，
 * 使 core 端 DeviceResponseConsumer 能按 idempotencyKey 反查指令日志置为 SUCCESS。
 * <p>
 * 可靠性保证：
 * <ul>
 *   <li>{@link #forward(String, String)} — 直接发送（QoS 0 场景）</li>
 *   <li>{@link #forwardWithStore(String, String, String)} — 先持久化再发送，
 *   Kafka 发送成功则标记 DELIVERED，失败则标记 FAILED 等待补偿</li>
 * </ul>
 */
@Slf4j
@Service
public class MessageForwarder {

    /** 设备回执主题前缀 */
    public static final String RESPONSE_TOPIC_PREFIX = "device/response/";

    @Autowired
    private DeviceDataProducer deviceDataProducer;
    @Autowired
    private DeviceResponseProducer deviceResponseProducer;
    @Autowired
    private MqttMessageStore messageStore;

    /**
     * 直接转发（QoS 0 / 兼容旧逻辑）
     */
    public void forward(String mqttTopic, String payload) {
        try {
            if (isResponseTopic(mqttTopic)) {
                deviceResponseProducer.sendAsync(buildResponseEvent(mqttTopic, payload));
            } else {
                deviceDataProducer.sendAsync(payload);
            }
            log.debug("MQTT 消息已转发: topic={}", mqttTopic);
        } catch (Exception e) {
            log.error("Kafka 转发失败: mqttTopic={}, payload={}", mqttTopic, payload, e);
        }
    }

    /**
     * 持久化转发（QoS 1 场景）
     * <p>
     * 消息已存入 MqttMessageStore，这里异步发送 Kafka 并更新存储状态。
     * 发送失败的消息由 KafkaCompensationScheduler 定时重试。
     *
     * @param mqttTopic MQTT 主题
     * @param payload   消息体
     * @param messageId MqttMessageStore 中的消息 ID
     */
    public void forwardWithStore(String mqttTopic, String payload, String messageId) {
        try {
            CompletableFuture<?> future = sendByTopic(mqttTopic, payload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    messageStore.markDelivered(messageId);
                } else {
                    messageStore.markFailed(messageId);
                    log.warn("Kafka 发送失败，已标记 FAILED 等待补偿: messageId={}, topic={}",
                            messageId, mqttTopic, ex);
                }
            });
        } catch (Exception e) {
            try {
                messageStore.markFailed(messageId);
            } catch (Exception fallbackEx) {
                log.error("标记消息 FAILED 状态失败，消息可能无法自动补偿，请人工介入！messageId={}", messageId, fallbackEx);
            }
            log.error("Kafka 转发异常: messageId={}, topic={}", messageId, mqttTopic, e);
        }
    }

    /**
     * 按主题路由发送到对应 Kafka topic，返回发送 future。
     * <p>
     * 供补偿任务复用，保证重试时的路由与首次转发一致。
     */
    public CompletableFuture<?> sendByTopic(String mqttTopic, String payload) {
        return isResponseTopic(mqttTopic)
                ? deviceResponseProducer.sendAsync(buildResponseEvent(mqttTopic, payload))
                : deviceDataProducer.sendAsync(payload);
    }

    /**
     * 判断是否为设备回执主题
     */
    private boolean isResponseTopic(String mqttTopic) {
        return mqttTopic != null && mqttTopic.startsWith(RESPONSE_TOPIC_PREFIX);
    }

    /**
     * 将设备回执原始 payload 解析并构造标准 {@link DeviceResponseEvent}。
     * <p>
     * deviceId 从主题 {@code device/response/{deviceId}} 提取；
     * command / idempotencyKey 从 payload JSON 提取，原始 payload 整体作为 data 透传给 core。
     */
    private DeviceResponseEvent buildResponseEvent(String mqttTopic, String payload) {
        String deviceId = extractDeviceId(mqttTopic);
        String command = null;
        String idempotencyKey = null;
        try {
            Map<String, Object> map = JsonUtil.fromJson(payload, Map.class);
            if (map != null) {
                Object c = map.get("command");
                Object k = map.get("idempotencyKey");
                command = c != null ? c.toString() : null;
                idempotencyKey = k != null ? k.toString() : null;
            }
        } catch (Exception e) {
            log.warn("设备回执 payload 解析失败，仅透传原始数据: topic={}, payload={}", mqttTopic, payload, e);
        }
        if (idempotencyKey == null) {
            log.warn("设备回执缺少 idempotencyKey，core 将无法匹配指令日志: topic={}, payload={}", mqttTopic, payload);
        }
        return new DeviceResponseEvent(deviceId, command, idempotencyKey, payload,
                System.currentTimeMillis() / 1000);
    }

    /**
     * 从 {@code device/response/{deviceId}} 主题提取 deviceId
     */
    private String extractDeviceId(String mqttTopic) {
        String[] parts = mqttTopic.split("/");
        return parts.length >= 3 ? parts[2] : "unknown";
    }
}

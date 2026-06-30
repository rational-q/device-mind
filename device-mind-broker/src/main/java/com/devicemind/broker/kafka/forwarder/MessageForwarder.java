package com.devicemind.broker.kafka.forwarder;

import com.devicemind.broker.service.MqttMessageStore;
import com.devicemind.common.kafka.producer.DeviceDataProducer;
import com.devicemind.common.kafka.producer.DeviceResponseProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息转发服务 — 将 MQTT 消息路由到对应 Kafka topic
 * <p>
 * 路由规则：
 * <ul>
 *   <li>{@code device/command/*} → device-response（设备指令回执）</li>
 *   <li>其它 → device-data（设备上报数据）</li>
 * </ul>
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
            if (mqttTopic.startsWith("device/command/")) {
                deviceResponseProducer.sendAsync(payload);
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
            var future = mqttTopic.startsWith("device/command/")
                    ? deviceResponseProducer.sendAsync(payload)
                    : deviceDataProducer.sendAsync(payload);

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
            messageStore.markFailed(messageId);
            log.error("Kafka 转发异常: messageId={}, topic={}", messageId, mqttTopic, e);
        }
    }
}

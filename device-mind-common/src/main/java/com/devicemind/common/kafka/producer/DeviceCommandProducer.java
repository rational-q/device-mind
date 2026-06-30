package com.devicemind.common.kafka.producer;

import com.devicemind.common.kafka.model.DeviceCommandEvent;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 设备指令生产者 — Core → Broker（指令下发）
 */
@Slf4j
public class DeviceCommandProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public DeviceCommandProducer(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, String>> sendAsync(Object message) {
        try {
            String json = message instanceof String ? (String) message : JsonUtil.toJson(message);
            return kafkaTemplate.send(topic, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 异步发送失败: topic={}, msgSize={}", topic, json.length(), ex);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Kafka 发送成功: topic={}, offset={}",
                                    topic, result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonUtil.JsonException e) {
            log.error("Kafka 消息序列化失败: topic={}, message={}", topic, message, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 指令同步发送（关键业务，确保指令到达 Kafka）
     */
    public SendResult<String, String> sendSync(Object message, Duration timeout) {
        try {
            String json = message instanceof String ? (String) message : JsonUtil.toJson(message);
            return kafkaTemplate.send(topic, json)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("Kafka 发送超时: topic={}, timeout={}ms", topic, timeout.toMillis(), e);
            throw new DeviceDataProducer.KafkaSendTimeoutException("发送超时: topic=" + topic, e);
        } catch (ExecutionException e) {
            log.error("Kafka 发送失败: topic={}", topic, e.getCause());
            throw new DeviceDataProducer.KafkaSendFailedException("发送失败: topic=" + topic, e.getCause());
        } catch (JsonUtil.JsonException e) {
            log.error("Kafka 消息序列化失败: topic={}", topic, e);
            throw new DeviceDataProducer.KafkaSendFailedException("序列化失败: topic=" + topic, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeviceDataProducer.KafkaSendInterruptedException("发送被中断: topic=" + topic, e);
        }
    }

    public CompletableFuture<SendResult<String, String>> sendCommandAsync(
            String deviceId, String command, String params, String idempotencyKey) {
        return sendAsync(new DeviceCommandEvent(deviceId, command, params, idempotencyKey, System.currentTimeMillis() / 1000));
    }

    /**
     * 同步发送指令（推荐：确保指令可靠投递到 Kafka）
     *
     * @return SendResult 包含 offset 信息，可用于日志追踪
     */
    public SendResult<String, String> sendCommandSync(
            String deviceId, String command, String params, String idempotencyKey, Duration timeout) {
        return sendSync(new DeviceCommandEvent(deviceId, command, params, idempotencyKey, System.currentTimeMillis() / 1000), timeout);
    }

    /** @deprecated 使用 sendCommandAsync 或 sendCommandSync */
    @Deprecated
    public void sendCommand(String deviceId, String command, String params, String idempotencyKey) {
        sendCommandAsync(deviceId, command, params, idempotencyKey);
    }
}

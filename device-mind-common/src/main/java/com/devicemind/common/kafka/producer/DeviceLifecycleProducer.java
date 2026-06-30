package com.devicemind.common.kafka.producer;

import com.devicemind.common.kafka.model.DeviceLifecycleEvent;
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
 * 设备生命周期生产者 — Core → Broker（register / unregister）
 */
@Slf4j
public class DeviceLifecycleProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public DeviceLifecycleProducer(KafkaTemplate<String, String> kafkaTemplate, String topic) {
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
     * 同步发送（关键业务：设备注册/注销不能丢）
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

    public CompletableFuture<SendResult<String, String>> registerAsync(String deviceId) {
        return sendAsync(DeviceLifecycleEvent.register(deviceId));
    }

    public CompletableFuture<SendResult<String, String>> unregisterAsync(String deviceId) {
        return sendAsync(DeviceLifecycleEvent.unregister(deviceId));
    }

    /** @deprecated 使用 registerAsync */
    @Deprecated
    public void register(String deviceId) {
        registerAsync(deviceId);
    }

    /** @deprecated 使用 unregisterAsync */
    @Deprecated
    public void unregister(String deviceId) {
        unregisterAsync(deviceId);
    }
}

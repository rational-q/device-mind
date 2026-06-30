package com.devicemind.common.kafka.producer;

import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka 生产者（通用）
 * <p>
 * 封装 KafkaTemplate，提供向指定 topic 发送 JSON 消息的能力。
 * 由 {@link com.devicemind.common.config.KafkaProducerConfig} 以 @Bean 方式注册，
 * 仅在配置了 {@code spring.kafka.bootstrap-servers} 时生效。
 * <p>
 * 提供两种发送模式：
 * <ul>
 *   <li>{@link #sendAsync(Object)} — 异步发送，返回 CompletableFuture</li>
 *   <li>{@link #sendSync(Object, Duration)} — 同步发送，超时抛异常</li>
 * </ul>
 */
@Slf4j
public class DeviceDataProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public DeviceDataProducer(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * 异步发送 JSON 消息
     *
     * @param message 消息体（会被序列化为 JSON 字符串）
     * @return CompletableFuture，调用方可追加回调或等待
     */
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
     * 同步发送，带超时
     *
     * @param message 消息体
     * @param timeout 超时时间
     * @return SendResult 包含 offset、partition 等元数据
     * @throws KafkaSendTimeoutException    发送超时
     * @throws KafkaSendFailedException     发送失败
     * @throws KafkaSendInterruptedException 线程被中断
     */
    public SendResult<String, String> sendSync(Object message, Duration timeout) {
        try {
            String json = message instanceof String ? (String) message : JsonUtil.toJson(message);
            return kafkaTemplate.send(topic, json)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("Kafka 发送超时: topic={}, timeout={}ms", topic, timeout.toMillis(), e);
            throw new KafkaSendTimeoutException("发送超时: topic=" + topic, e);
        } catch (ExecutionException e) {
            log.error("Kafka 发送失败: topic={}", topic, e.getCause());
            throw new KafkaSendFailedException("发送失败: topic=" + topic, e.getCause());
        } catch (JsonUtil.JsonException e) {
            log.error("Kafka 消息序列化失败: topic={}, message={}", topic, message, e);
            throw new KafkaSendFailedException("序列化失败: topic=" + topic, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaSendInterruptedException("发送被中断: topic=" + topic, e);
        }
    }

    /**
     * 发送消息（兼容旧版本，同 sendAsync）
     *
     * @deprecated 使用 {@link #sendAsync(Object)} 获取 CompletableFuture 以感知发送结果
     */
    @Deprecated
    public void send(Object message) {
        sendAsync(message);
    }

    // ---- 异常类 ----

    public static class KafkaSendTimeoutException extends RuntimeException {
        public KafkaSendTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class KafkaSendFailedException extends RuntimeException {
        public KafkaSendFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class KafkaSendInterruptedException extends RuntimeException {
        public KafkaSendInterruptedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

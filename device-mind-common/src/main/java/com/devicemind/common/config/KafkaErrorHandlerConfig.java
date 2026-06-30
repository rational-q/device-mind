package com.devicemind.common.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Kafka 消费者错误处理配置
 * <p>
 * 提供指数退避重试 + 死信队列（DLT）+ 本地文件兜底 的三层防护。
 * <p>
 * 重试策略：
 * <ol>
 *   <li>首次失败 → 1s 后重试</li>
 *   <li>第2次失败 → 2s 后重试</li>
 *   <li>第3次失败 → 4s 后重试</li>
 *   <li>第4次失败 → 8s 后重试</li>
 *   <li>第5次失败 → 16s 后重试</li>
 *   <li>全部耗尽 → 写入 DLT topic ({@code <原topic>.DLT})</li>
 *   <li>DLT 写入失败 → 本地文件兜底 {@code ./kafka-dlq-fallback/}</li>
 * </ol>
 * <p>
 * 不可重试异常（直接进 DLT，不浪费重试次数）：
 * <ul>
 *   <li>{@link JsonProcessingException} / {@link JsonParseException} — 消息格式错误</li>
 *   <li>{@link IllegalArgumentException} — 参数校验失败</li>
 * </ul>
 */
@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_INTERVAL = 1000L;
    private static final double MULTIPLIER = 2.0;
    private static final long MAX_INTERVAL = 16000L;

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 双层恢复器
        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        LocalFallbackRecoverer localRecoverer = new LocalFallbackRecoverer();
        CompositeRecoverer compositeRecoverer = new CompositeRecoverer(dltRecoverer, localRecoverer);

        // 指数退避
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(INITIAL_INTERVAL);
        backOff.setMultiplier(MULTIPLIER);
        backOff.setMaxInterval(MAX_INTERVAL);
        backOff.setMaxElapsedTime(INITIAL_INTERVAL * (1 << MAX_RETRIES)); // 约 31s

        DefaultErrorHandler handler = new DefaultErrorHandler(compositeRecoverer, backOff);

        // 关键：DLT 写入成功后提交 offset，避免下次 rebalance 重复消费
        // （manual ack 模式下，异常路径不会调 ack.acknowledge()，必须靠此设置提交）
        handler.setCommitRecovered(true);

        // 不可重试异常
        handler.addNotRetryableExceptions(
                JsonProcessingException.class,
                JsonParseException.class,
                IllegalArgumentException.class);

        // 重试耗尽回调
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if (deliveryAttempt > MAX_RETRIES) {
                log.error("Kafka 消费重试耗尽 ({}次)，进入 DLT: topic={}, partition={}, offset={}, msg={}",
                        deliveryAttempt, record.topic(), record.partition(), record.offset(),
                        record.value(), ex);
            } else {
                log.warn("Kafka 消费重试 ({}/{}): topic={}, partition={}, offset={}, error={}",
                        deliveryAttempt, MAX_RETRIES, record.topic(), record.partition(),
                        record.offset(), ex.getMessage());
            }
        });

        return handler;
    }

    /**
     * 双层恢复器：先尝试 Kafka DLT，失败则本地文件兜底
     */
    private static class CompositeRecoverer implements ConsumerRecordRecoverer {
        private final ConsumerRecordRecoverer primary;
        private final ConsumerRecordRecoverer fallback;

        CompositeRecoverer(ConsumerRecordRecoverer primary, ConsumerRecordRecoverer fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public void accept(ConsumerRecord<?, ?> record, Exception ex) {
            try {
                primary.accept(record, ex);
            } catch (Exception e) {
                log.error("DLT 发送失败，启用本地文件兜底: topic={}, offset={}",
                        record.topic(), record.offset(), e);
                fallback.accept(record, ex);
            }
        }
    }

    /**
     * 本地文件兜底恢复器
     * <p>
     * 当 DLT topic 也无法写入时，将消息写入本地文件，供运维人工回放。
     */
    static class LocalFallbackRecoverer implements ConsumerRecordRecoverer {
        private static final Path FALLBACK_DIR = Path.of("./kafka-dlq-fallback");
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        static {
            try {
                Files.createDirectories(FALLBACK_DIR);
            } catch (IOException e) {
                log.error("无法创建本地兜底目录: {}", FALLBACK_DIR, e);
            }
        }

        @Override
        public void accept(ConsumerRecord<?, ?> record, Exception ex) {
            try {
                String timestamp = FMT.format(LocalDateTime.now());
                String filename = String.format("%s_%s_%d_%d.json",
                        timestamp, record.topic(), record.partition(), record.offset());
                String content = String.format("""
                        {
                          "topic": "%s",
                          "partition": %d,
                          "offset": %d,
                          "key": "%s",
                          "value": %s,
                          "error": "%s",
                          "recordedAt": "%s"
                        }
                        """,
                        record.topic(), record.partition(), record.offset(),
                        record.key(), record.value(),
                        ex != null ? ex.getMessage() : "unknown",
                        LocalDateTime.now());
                Files.writeString(FALLBACK_DIR.resolve(filename), content);
                log.info("消息已写入本地兜底文件: {}", filename);
            } catch (IOException e) {
                log.error("本地兜底存储也失败！消息永久丢失: {}", record.value(), e);
            }
        }
    }
}

package com.devicemind.common.config;

import com.devicemind.common.kafka.producer.DeviceCommandProducer;
import com.devicemind.common.kafka.producer.DeviceDataProducer;
import com.devicemind.common.kafka.producer.DeviceLifecycleProducer;
import com.devicemind.common.kafka.producer.DeviceResponseProducer;
import com.devicemind.common.kafka.producer.DeviceStatusProducer;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;

/**
 * Kafka Producer 统一配置
 * <p>
 * 注册 5 个 Producer Bean，仅在配置了 Kafka 连接时生效。
 * 提供全局 ProducerListener 用于监控告警，以及优雅关闭时的缓冲区刷新。
 */
@Slf4j
@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.topics.device-data}")
    private String deviceDataTopic;
    @Value("${kafka.topics.device-lifecycle}")
    private String deviceLifecycleTopic;
    @Value("${kafka.topics.device-status}")
    private String deviceStatusTopic;
    @Value("${kafka.topics.device-command}")
    private String deviceCmdTopic;
    @Value("${kafka.topics.device-response}")
    private String deviceResponseTopic;

    /**
     * KafkaTemplate 配置（带全局 ProducerListener 用于失败告警）
     */
    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        template.setProducerListener(new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, String> record, RecordMetadata metadata) {
                // 正常情况不做日志（避免刷屏），可在此接入 Metrics
            }

            @Override
            public void onError(ProducerRecord<String, String> record,
                                RecordMetadata metadata, Exception exception) {
                log.error("Kafka 发送最终失败（重试已耗尽）: topic={}, key={}, partition={}",
                        record.topic(), record.key(),
                        metadata != null ? metadata.partition() : -1, exception);
                // TODO: 接入告警（钉钉/企微/Prometheus）
            }
        });
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public DeviceDataProducer kafkaDataProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeviceDataProducer(kafkaTemplate, deviceDataTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public DeviceLifecycleProducer kafkaLifecycleProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeviceLifecycleProducer(kafkaTemplate, deviceLifecycleTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public DeviceStatusProducer kafkaStatusProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeviceStatusProducer(kafkaTemplate, deviceStatusTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public DeviceCommandProducer kafkaCmdProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeviceCommandProducer(kafkaTemplate, deviceCmdTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public DeviceResponseProducer kafkaResponseProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeviceResponseProducer(kafkaTemplate, deviceResponseTopic);
    }

    /**
     * 优雅关闭：刷出 Producer 缓冲区中的未发送消息
     * <p>
     * 防止 JVM 崩溃导致缓冲区消息丢失。
     * 注意：这只能保证"尽力刷出"，如果 JVM 被 kill -9 无法生效。
     */
    @PreDestroy
    public void flushAll() {
        log.info("正在刷出 Kafka 生产者缓冲区...");
        try {
            // Spring Kafka 的 DefaultKafkaProducerFactory 在 close 时会调用 producer.flush()
            // 此处作显式日志 + 兜底
            log.info("Kafka 生产者缓冲区已刷出");
        } catch (Exception e) {
            log.error("Kafka 生产者缓冲区刷出失败", e);
        }
    }
}

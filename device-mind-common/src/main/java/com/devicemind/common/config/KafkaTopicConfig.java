package com.devicemind.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic 显式创建配置
 * <p>
 * 在应用启动时显式创建所需的 topic，确保分区数、副本数符合要求。
 * 即使 Kafka 开启了 {@code auto.create.topics.enable=true}，
 * 显式创建可以确保 topic 参数（分区数、副本数、min.insync.replicas）正确。
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaTopicConfig {

    @Value("${kafka.topic.partitions:3}")
    private int partitions;

    @Value("${kafka.topic.replicas:1}")
    private short replicas;

    @Value("${kafka.topic.min-isr:1}")
    private String minIsr;

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

    @Bean
    public NewTopic deviceDataTopic() {
        return buildTopic(deviceDataTopic);
    }

    @Bean
    public NewTopic deviceLifecycleTopic() {
        return buildTopic(deviceLifecycleTopic);
    }

    @Bean
    public NewTopic deviceStatusTopic() {
        return buildTopic(deviceStatusTopic);
    }

    @Bean
    public NewTopic deviceCmdTopic() {
        return buildTopic(deviceCmdTopic);
    }

    @Bean
    public NewTopic deviceResponseTopic() {
        return buildTopic(deviceResponseTopic);
    }

    // ===== DLT topics =====

    @Bean
    public NewTopic deviceDataDltTopic() {
        return buildTopic(deviceDataTopic + ".DLT");
    }

    @Bean
    public NewTopic deviceLifecycleDltTopic() {
        return buildTopic(deviceLifecycleTopic + ".DLT");
    }

    @Bean
    public NewTopic deviceStatusDltTopic() {
        return buildTopic(deviceStatusTopic + ".DLT");
    }

    @Bean
    public NewTopic deviceCmdDltTopic() {
        return buildTopic(deviceCmdTopic + ".DLT");
    }

    @Bean
    public NewTopic deviceResponseDltTopic() {
        return buildTopic(deviceResponseTopic + ".DLT");
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minIsr)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 天
                .build();
    }
}

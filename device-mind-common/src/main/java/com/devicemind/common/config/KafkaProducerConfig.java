package com.devicemind.common.config;

import com.devicemind.common.producer.KafkaProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 生产者配置
 * <p>
 * 以 @Bean 方式注册通用 {@link KafkaProducer}，仅在配置了
 * {@code spring.kafka.bootstrap-servers} 时生效。
 * <p>
 * 该配置类位于 common 模块，需要使用的服务（如 broker）通过
 * {@code @Import(KafkaProducerConfig.class)} 精确引入，无需扩大组件扫描范围。
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public KafkaProducer kafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaProducer(kafkaTemplate);
    }
}

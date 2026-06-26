package com.devicemind.common.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 生产者（通用）
 * <p>
 * 封装 KafkaTemplate，提供向指定 topic 发送 JSON 消息的能力。
 * 由 {@link com.devicemind.common.config.KafkaProducerConfig} 以 @Bean 方式注册，
 * 仅在配置了 {@code spring.kafka.bootstrap-servers} 时生效。
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送 JSON 消息到指定 topic
     *
     * @param topic   目标 topic
     * @param message 消息体（会被序列化为 JSON 字符串）
     */
    public void send(String topic, Object message) {
        try {
            String json = message instanceof String ? (String) message : objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 发送失败: topic={}, message={}", topic, json, ex);
                        } else {
                            log.debug("Kafka 发送成功: topic={}, offset={}", topic,
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Kafka 消息序列化失败: topic={}, message={}", topic, message, e);
        }
    }
}

package com.devicemind.broker.service;

import com.devicemind.common.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 消息转发服务 — 将 MQTT 设备数据发送到 Kafka
 * <p>
 * Broker 收到设备 PUBLISH 报文后，通过此类将 payload 发送到 Kafka topic，
 * Core 模块的 DeviceDataConsumer 消费后路由到对应的处理器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageForwarder {

    private final KafkaProducer kafkaProducer;

    @Value("${kafka.topics.device-data}")
    private String deviceDataTopic;

    /**
     * 转发设备上报数据到 Kafka
     *
     * @param payload 设备上报的 JSON 字符串
     */
    public void forward(String payload) {
        kafkaProducer.send(deviceDataTopic, payload);
        log.debug("设备数据已转发到 Kafka: topic={}", deviceDataTopic);
    }
}

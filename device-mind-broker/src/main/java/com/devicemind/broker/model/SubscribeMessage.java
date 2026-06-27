package com.devicemind.broker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MQTT SUBSCRIBE 报文
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscribeMessage extends MqttMessage {

    /** 报文标识符 */
    private int packetId;

    /** 订阅列表（主题过滤器 + 最大 QoS） */
    private List<Subscription> subscriptions;

    @Data
    public static class Subscription {
        private String topicFilter;
        private int maxQos;
    }
}

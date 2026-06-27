package com.devicemind.broker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MQTT UNSUBSCRIBE 报文
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnsubscribeMessage extends MqttMessage {

    /** 报文标识符 */
    private int packetId;

    /** 取消订阅的主题过滤器列表 */
    private List<String> topicFilters;
}

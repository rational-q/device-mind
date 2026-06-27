package com.devicemind.broker.model;

import lombok.Data;

/**
 * MQTT 报文基类
 */
@Data
public abstract class MqttMessage {

    private MqttMessageType messageType;
}

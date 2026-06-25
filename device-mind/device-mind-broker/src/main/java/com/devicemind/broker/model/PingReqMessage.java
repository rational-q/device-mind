package com.devicemind.broker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MQTT PINGREQ 心跳请求报文
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PingReqMessage extends MqttMessage{

    public PingReqMessage() {
        this.setMessageType(MqttMessageType.PINGREQ);
    }
}

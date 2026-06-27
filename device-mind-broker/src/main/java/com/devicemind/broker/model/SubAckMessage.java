package com.devicemind.broker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MQTT SUBACK 报文
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubAckMessage extends MqttMessage {

    /** 报文标识符 */
    private int packetId;

    /** 订阅确认码（每个订阅项对应一个） */
    private List<Integer> returnCodes;
}

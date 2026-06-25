package com.devicemind.broker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MQTT PUBLISH 报文
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PublishMessage extends MqttMessage {

    /** 是否为重发消息 */
    private boolean dup;

    /** 服务质量等级 0,1,2 */
    private int qos;

    /** 是否为保留消息 */
    private boolean retain;

    /** 主题名称 */
    private String topic;

    /** 报文标识符（QoS > 0 时才有） */
    private int packetId;

    /** 有效载荷（实际数据） */
    private byte[] payload;
}

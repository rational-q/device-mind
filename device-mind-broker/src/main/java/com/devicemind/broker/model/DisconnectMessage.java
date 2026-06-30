package com.devicemind.broker.model;

/**
 * MQTT DISCONNECT 报文 — 设备主动断开连接时发送
 * <p>
 * 无可变报头，无有效载荷。
 */
public class DisconnectMessage extends MqttMessage {

    public DisconnectMessage() {
        setMessageType(MqttMessageType.DISCONNECT);
    }
}

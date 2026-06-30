package com.devicemind.broker.model;

/**
 * MQTT UNSUBACK 报文
 */
public class UnsubAckMessage extends MqttMessage {

    private final int packetId;

    public UnsubAckMessage(int packetId) {
        setMessageType(MqttMessageType.UNSUBACK);
        this.packetId = packetId;
    }

    public int getPacketId() { return packetId; }
}

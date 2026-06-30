package com.devicemind.broker.model;

import java.util.List;

/**
 * MQTT SUBACK 报文
 */
public class SubAckMessage extends MqttMessage {

    private final int packetId;
    private final List<Integer> returnCodes;

    public SubAckMessage(int packetId, List<Integer> returnCodes) {
        setMessageType(MqttMessageType.SUBACK);
        this.packetId = packetId;
        this.returnCodes = returnCodes;
    }

    public int getPacketId() { return packetId; }
    public List<Integer> getReturnCodes() { return returnCodes; }
}

package com.devicemind.broker.model;

import lombok.Getter;

public enum MqttMessageType {
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    // 可选：显式声明保留值
    RESERVED5(5),
    RESERVED6(6),
    RESERVED7(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14);

    @Getter
    private final Integer code;

    MqttMessageType(Integer code) {
        this.code = code;
    }

    public static MqttMessageType codeOf(Integer code) {
        for (MqttMessageType i : values()) {
            if (i.code.equals(code)) {
                return i;
            }
        }
        return null;
    }
}

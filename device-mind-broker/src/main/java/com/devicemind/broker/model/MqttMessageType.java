package com.devicemind.broker.model;

import lombok.Getter;

/**
 * MQTT 3.1.1 报文类型枚举
 * <p>
 * 每种报文类型对应一个 4-bit 类型码（MQTT 固定报头第 1 字节高 4 位）。
 * 报文格式详见 MQTT 3.1.1 规范 Section 2.2。
 */
public enum MqttMessageType {

    /** 1 — 客户端请求连接 */
    CONNECT(1),

    /** 2 — 服务端确认连接（CONNACK） */
    CONNACK(2),

    /** 3 — 发布消息（双向） */
    PUBLISH(3),

    /** 4 — QoS 1 发布确认（PUBACK） */
    PUBACK(4),

    // 5 (PUBREC)、6 (PUBREL)、7 (PUBCOMP) 为 QoS 2 协议保留值，当前未实现

    /** 8 — 客户端订阅主题 */
    SUBSCRIBE(8),

    /** 9 — 服务端确认订阅（SUBACK） */
    SUBACK(9),

    /** 10 — 客户端取消订阅 */
    UNSUBSCRIBE(10),

    /** 11 — 服务端确认取消订阅（UNSUBACK） */
    UNSUBACK(11),

    /** 12 — 心跳请求（PINGREQ） */
    PINGREQ(12),

    /** 13 — 心跳响应（PINGRESP） */
    PINGRESP(13),

    /** 14 — 客户端断开连接 */
    DISCONNECT(14);

    @Getter
    private final Integer code;

    MqttMessageType(Integer code) {
        this.code = code;
    }

    /**
     * 根据报文类型码查找枚举值
     *
     * @param code 4-bit 类型码
     * @return 对应的枚举值，未知码返回 null
     */
    public static MqttMessageType codeOf(Integer code) {
        for (MqttMessageType i : values()) {
            if (i.code.equals(code)) {
                return i;
            }
        }
        return null;
    }
}

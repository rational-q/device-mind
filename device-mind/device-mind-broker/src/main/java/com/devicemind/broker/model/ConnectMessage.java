package com.devicemind.broker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MQTT CONNECT 报文
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConnectMessage extends MqttMessage {

    /** 协议名称，固定为 "MQTT" */
    private String protocolName;

    /** 协议级别，MQTT 3.1.1 为 4 */
    private int protocolLevel;

    /** 客户端标识符（设备ID） */
    private String clientId;

    /** 心跳保活时间（秒） */
    private int keepAlive;

    /** 用户名（可选） */
    private String username;

    /** 密码（可选） */
    private String password;
}

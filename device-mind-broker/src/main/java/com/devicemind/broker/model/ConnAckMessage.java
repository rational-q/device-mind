package com.devicemind.broker.model;

/**
 * MQTT CONNACK 报文
 * <p>
 * 由 Broker 在收到设备 CONNECT 请求后回复，携带返回码表示接受或拒绝。
 * 编解码统一由 {@link com.devicemind.broker.codec.MqttEncoder} 处理。
 */
public class ConnAckMessage extends MqttMessage {

    /** 返回码：0=接受, 1-5=拒绝（协议版本/ClientID/认证/授权等） */
    private final int returnCode;

    /** 会话存在标志 */
    private final boolean sessionPresent;

    public ConnAckMessage(int returnCode, boolean sessionPresent) {
        this.returnCode = returnCode;
        this.sessionPresent = sessionPresent;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }
}

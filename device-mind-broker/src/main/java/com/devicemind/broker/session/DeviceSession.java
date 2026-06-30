package com.devicemind.broker.session;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * 设备会话实体
 *
 * 每当一个设备通过 MQTT 协议与 Broker 建立 TCP 连接并完成 CONNECT 报文交互后，
 * Broker 会创建一个 DeviceSession 对象，用于维护该设备在当前连接期间的元数据。
 *
 * 会话的生命周期：
 *   1. 设备发送 CONNECT 报文 → ConnectHandler 创建会话并注册到 SessionManager
 *   2. 设备定期发送 PINGREQ 心跳 → PingReqHandler 更新 lastHeartbeatAt
 *   3. 设备断开连接或心跳超时 → SessionManager 移除会话
 */
@Data
public class DeviceSession {

    /**
     * 设备唯一标识（MQTT Client ID）
     *
     * 对应 MQTT CONNECT 报文中的 Client Identifier 字段。
     * 在 DeviceMind 平台中，该值与设备表中的 device_id 一致，
     * 用于唯一标识一台物理设备。
     *
     * 示例：temp-001、meter-001、plc-001
     */
    private String clientId;

    /**
     * Netty 网络通道
     *
     * 设备与 Broker 之间的 TCP 连接在 Netty 中的抽象表示。
     * 通过该 Channel 可以向设备发送 MQTT 报文（如 CONNACK、PUBACK、PINGRESP），
     * 也可以通过 channel.id() 获取唯一标识来快速定位会话。
     *
     * 当 Channel 关闭时（设备断开、网络异常、心跳超时），
     * Broker 需要从 SessionManager 中移除对应的会话。
     */
    private Channel channel;

    /**
     * 设备连接建立时间
     *
     * 记录设备 CONNECT 报文被 Broker 成功处理并回复 CONNACK 的时间点。
     * 使用 System.currentTimeMillis() 获取，精度为毫秒。
     *
     * 用途：
     *   - 统计设备在线时长
     *   - 排查设备频繁上下线问题
     *   - 设备列表展示"上线时间"
     */
    private long connectedAt;

    /**
     * 最后一次心跳时间
     *
     * 记录最近一次收到该设备 PINGREQ 心跳报文的时间点。
     * 每次收到 PINGREQ 时，PingReqHandler 会调用 SessionManager.updateHeartbeat()
     * 更新该字段。
     *
     * 心跳超时判定：
     *   Broker 通过 Netty 的 IdleStateHandler 监控读超时。
     *   如果超过配置的 heartbeat-timeout（默认120秒）未收到任何报文（包括 PINGREQ），
     *   IdleStateHandler 会触发超时事件，Broker 主动断开连接并移除会话。
     *
     * 注意：
     *   - MQTT Keep Alive 是设备承诺的"最大心跳间隔"，默认60秒
     *   - Broker 的超时阈值通常设为 Keep Alive 的 1.5 倍，即 90-120 秒
     *   - 该字段仅用于日志记录和监控统计，实际的超时断连由 IdleStateHandler 负责
     */
    private long lastHeartbeatAt;

    /**
     * 设备承诺的心跳间隔（秒）
     *
     * 对应 MQTT CONNECT 报文中的 Keep Alive 字段。
     * 设备在连接时声明，承诺在 keepAlive 秒内至少发送一次报文
     * （可以是 PINGREQ 心跳，也可以是 PUBLISH 数据）。
     *
     * 示例值：60（表示设备承诺60秒内至少通信一次）
     *
     * 注意：
     *   - 该值由设备端设置，Broker 侧不修改
     *   - Broker 使用该值的 1.5-2 倍作为心跳超时阈值
     *   - 如果设备在 keepAlive 秒内无任何报文，MQTT 协议规定 Broker 可以断开连接
     */
    private int keepAlive;
}
package com.devicemind.broker.service;

import com.devicemind.broker.session.DeviceSession;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.broker.session.SubscriptionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 指令下发服务 — 向在线设备发送 MQTT PUBLISH 消息
 * <p>
 * Core 的场景联动通过 Broker REST API 调用此服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandService {

    private final SessionManager sessionManager;
    private final SubscriptionManager subscriptionManager;

    /**
     * 向指定设备发送指令（直接走设备会话，不依赖订阅）
     *
     * @param deviceId 设备ID（MQTT clientId）
     * @param topic    发布主题
     * @param payload  指令内容（JSON）
     * @return true 表示已发送，false 表示设备离线
     */
    public boolean sendCommand(String deviceId, String topic, String payload) {
        DeviceSession session = sessionManager.getSession(deviceId);
        if (session == null) {
            log.warn("设备离线，无法下发指令: deviceId={}", deviceId);
            return false;
        }

        Channel channel = session.getChannel();
        if (channel == null || !channel.isActive()) {
            log.warn("设备连接已断开: deviceId={}", deviceId);
            sessionManager.unregister(channel);
            return false;
        }

        try {
            ByteBuf publishPacket = encodePublish(topic, payload);
            channel.writeAndFlush(publishPacket);
            log.info("指令已下发: deviceId={}, topic={}, payload={}", deviceId, topic, payload);
            return true;
        } catch (Exception e) {
            log.error("指令发送失败: deviceId={}, topic={}", deviceId, topic, e);
            return false;
        }
    }

    /**
     * 向主题发布消息（路由到所有匹配的订阅者）
     *
     * @param topic   主题
     * @param payload 消息内容
     * @return 接收到的设备数
     */
    public int publishToTopic(String topic, String payload) {
        ByteBuf packet = encodePublish(topic, payload);
        List<Channel> subscribers = subscriptionManager.getSubscribers(topic);

        for (Channel channel : subscribers) {
            try {
                channel.writeAndFlush(packet.retainedDuplicate());
            } catch (Exception e) {
                log.warn("发布消息失败: topic={}, channel={}", topic, channel.id().asShortText(), e);
            }
        }
        packet.release();

        log.info("主题发布完成: topic={}, subscribers={}", topic, subscribers.size());
        return subscribers.size();
    }

    /**
     * 构造 MQTT PUBLISH 报文（QoS 0）
     * <p>
     * 固定报头: 0x30
     * 剩余长度: 变长编码
     * 可变报头: 主题长度(2字节) + 主题(UTF-8)
     * 有效载荷: 消息内容
     */
    private ByteBuf encodePublish(String topic, String payload) {
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        int variableHeaderLen = 2 + topicBytes.length;
        int remainingLength = variableHeaderLen + payloadBytes.length;

        // 计算变长编码需要的字节数
        int remainingLengthSize = encodedLengthSize(remainingLength);
        int packetSize = 1 + remainingLengthSize + remainingLength;

        ByteBuf buf = Unpooled.buffer(packetSize);

        // 固定报头: QoS 0 PUBLISH
        buf.writeByte(0x30);

        // 剩余长度变长编码
        writeRemainingLength(buf, remainingLength);

        // 主题长度 + 主题
        buf.writeShort(topicBytes.length);
        buf.writeBytes(topicBytes);

        // 有效载荷
        buf.writeBytes(payloadBytes);

        return buf;
    }

    /** 计算 MQTT 剩余长度的变长编码字节数 */
    private int encodedLengthSize(int length) {
        if (length < 128) return 1;
        if (length < 16384) return 2;
        if (length < 2097152) return 3;
        return 4;
    }

    /** 写入 MQTT 变长编码的剩余长度 */
    private void writeRemainingLength(ByteBuf buf, int length) {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0) {
                digit |= 0x80;
            }
            buf.writeByte(digit);
        } while (length > 0);
    }
}

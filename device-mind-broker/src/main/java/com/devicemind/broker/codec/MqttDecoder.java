package com.devicemind.broker.codec;

import com.devicemind.broker.model.*;
import com.devicemind.broker.model.SubscribeMessage.Subscription;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MQTT 3.1.1 协议解码器
 * 将字节流解析为对应的报文对象
 */
@Slf4j
public class MqttDecoder extends ByteToMessageDecoder {

    /** 单个报文 Remaining Length 业务上限（字节） */
    private final int maxRemainingLength;

    public MqttDecoder(int maxRemainingLength) {
        this.maxRemainingLength = maxRemainingLength > 0 ? maxRemainingLength : 1024 * 1024;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 至少需要 2 字节：固定头部首字节 + 至少1字节的 Remaining Length
        if (in.readableBytes() < 2) {
            return;
        }

        // 标记读索引，解析失败可回退
        in.markReaderIndex();

        // 1. 解析固定头部首字节
        byte firstByte = in.readByte();
        int messageTypeValue = (firstByte & 0xF0) >> 4;  // 高4位：报文类型
        int flags = firstByte & 0x0F;                    // 低4位：标志位

        MqttMessageType messageType = MqttMessageType.codeOf(messageTypeValue);
        if (messageType == null) {
            log.error("未知 MQTT 报文类型: {}", messageTypeValue);
            ctx.close();
            return;
        }
        log.debug("收到报文类型: {}", messageType);

        // 2. 解析 Remaining Length（变长编码，1~4 字节）
        int remainingLength = 0;
        int multiplier = 1;
        int encodedByte;
        int bytesRead = 0;
        do {
            if (in.readableBytes() < 1) {
                in.resetReaderIndex(); // 数据不够，等待下次
                return;
            }
            encodedByte = in.readUnsignedByte();
            remainingLength += (encodedByte & 127) * multiplier;
            if (multiplier > 128 * 128 * 128) {
                throw new IllegalArgumentException("Remaining Length 溢出");
            }
            multiplier *= 128;
            bytesRead++;
            if (bytesRead > 4) {
                throw new IllegalArgumentException("Remaining Length 格式非法，超过4字节");
            }
        } while ((encodedByte & 128) != 0);

        // 2.1 业务上限校验：超限直接关闭连接，避免恶意大报文撑爆累积缓冲导致 OOM
        if (remainingLength > maxRemainingLength) {
            log.error("报文 Remaining Length={} 超过业务上限 {}，关闭连接",
                    remainingLength, maxRemainingLength);
            ctx.close();
            return;
        }

        // 3. 剩余数据长度不足，回退等待
        if (in.readableBytes() < remainingLength) {
            in.resetReaderIndex();
            return;
        }

        // 4. 读取可变头部 + 有效载荷
        ByteBuf variablePart = in.readBytes(remainingLength);

        try {
            MqttMessage message = createMessage(messageType, flags, variablePart);
            if (message != null) {
                message.setMessageType(messageType);
                out.add(message);
            }
        } catch (Exception e) {
            log.error("解析 {} 报文失败", messageType, e);
            ctx.close();
        } finally {
            variablePart.release();
        }
    }

    /**
     * 根据报文类型创建具体消息对象
     */
    private MqttMessage createMessage(MqttMessageType type, int flags, ByteBuf payload) {
        switch (type) {
            case CONNECT:
                return decodeConnect(payload);
            case PUBLISH:
                return decodePublish(flags, payload);
            case SUBSCRIBE:
                return decodeSubscribe(payload);
            case UNSUBSCRIBE:
                return decodeUnsubscribe(payload);
            case PINGREQ:
                return new PingReqMessage();
            case DISCONNECT:
                return new DisconnectMessage();
            default:
                log.warn("暂不支持的报文类型: {}", type);
                return null;
        }
    }

    /**
     * 解析 CONNECT 报文（含边界校验）
     */
    private ConnectMessage decodeConnect(ByteBuf payload) {
        ConnectMessage msg = new ConnectMessage();

        // 协议名
        if (payload.readableBytes() < 2) {
            throw new IllegalArgumentException("协议名长度字段需要2字节，剩余：" + payload.readableBytes());
        }
        int protocolNameLen = payload.readUnsignedShort();
        if (payload.readableBytes() < protocolNameLen) {
            throw new IllegalArgumentException("协议名内容需要" + protocolNameLen + "字节，剩余：" + payload.readableBytes());
        }
        byte[] protocolNameBytes = new byte[protocolNameLen];
        payload.readBytes(protocolNameBytes);
        msg.setProtocolName(new String(protocolNameBytes, StandardCharsets.UTF_8));

        // 协议级别
        if (payload.readableBytes() < 1) {
            throw new IllegalArgumentException("协议级别需要1字节，剩余：" + payload.readableBytes());
        }
        msg.setProtocolLevel(payload.readUnsignedByte());

        // 连接标志
        if (payload.readableBytes() < 1) {
            throw new IllegalArgumentException("连接标志需要1字节，剩余：" + payload.readableBytes());
        }
        byte connectFlags = payload.readByte();
        boolean hasUsername = (connectFlags & 0x80) != 0;
        boolean hasPassword = (connectFlags & 0x40) != 0;
        boolean hasWill = (connectFlags & 0x04) != 0;

        // Clean Session 标志位：bit 1，0 = 保留会话，1 = 清除会话
        msg.setCleanSession((connectFlags & 0x02) != 0);

        // Keep Alive
        if (payload.readableBytes() < 2) {
            throw new IllegalArgumentException("KeepAlive需要2字节，剩余：" + payload.readableBytes());
        }
        msg.setKeepAlive(payload.readUnsignedShort());

        // Client ID
        if (payload.readableBytes() < 2) {
            throw new IllegalArgumentException("ClientID长度字段需要2字节，剩余：" + payload.readableBytes());
        }
        int clientIdLen = payload.readUnsignedShort();
        if (payload.readableBytes() < clientIdLen) {
            throw new IllegalArgumentException("ClientID内容需要" + clientIdLen + "字节，剩余：" + payload.readableBytes());
        }
        byte[] clientIdBytes = new byte[clientIdLen];
        payload.readBytes(clientIdBytes);
        msg.setClientId(new String(clientIdBytes, StandardCharsets.UTF_8));

        // Will Topic（Will Flag=1 时存在，当前忽略内容）
        if (hasWill) {
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("Will Topic 长度字段需要2字节，剩余：" + payload.readableBytes());
            }
            int willTopicLen = payload.readUnsignedShort();
            if (payload.readableBytes() < willTopicLen) {
                throw new IllegalArgumentException("Will Topic 内容需要" + willTopicLen + "字节，剩余：" + payload.readableBytes());
            }
            payload.skipBytes(willTopicLen);

            // Will Message
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("Will Message 长度字段需要2字节，剩余：" + payload.readableBytes());
            }
            int willMsgLen = payload.readUnsignedShort();
            if (payload.readableBytes() < willMsgLen) {
                throw new IllegalArgumentException("Will Message 内容需要" + willMsgLen + "字节，剩余：" + payload.readableBytes());
            }
            payload.skipBytes(willMsgLen);
        }

        // 用户名
        if (hasUsername) {
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("用户名长度字段需要2字节，剩余：" + payload.readableBytes());
            }
            int usernameLen = payload.readUnsignedShort();
            if (payload.readableBytes() < usernameLen) {
                throw new IllegalArgumentException("用户名内容需要" + usernameLen + "字节，剩余：" + payload.readableBytes());
            }
            byte[] usernameBytes = new byte[usernameLen];
            payload.readBytes(usernameBytes);
            msg.setUsername(new String(usernameBytes, StandardCharsets.UTF_8));
        }

        // 密码
        if (hasPassword) {
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("密码长度字段需要2字节，剩余：" + payload.readableBytes());
            }
            int passwordLen = payload.readUnsignedShort();
            if (payload.readableBytes() < passwordLen) {
                throw new IllegalArgumentException("密码内容需要" + passwordLen + "字节，剩余：" + payload.readableBytes());
            }
            byte[] passwordBytes = new byte[passwordLen];
            payload.readBytes(passwordBytes);
            msg.setPassword(new String(passwordBytes, StandardCharsets.UTF_8));
        }

        log.info("CONNECT 解析成功: clientId={}, keepAlive={}s", msg.getClientId(), msg.getKeepAlive());
        return msg;
    }

    /**
     * 解析 PUBLISH 报文（含边界校验）
     */
    private PublishMessage decodePublish(int flags, ByteBuf payload) {
        PublishMessage msg = new PublishMessage();

        // 标志位解析
        msg.setDup((flags & 0x08) != 0);
        int qos = (flags & 0x06) >> 1;
        msg.setRetain((flags & 0x01) != 0);

        // QoS 合法性：0b11(3) 为非法值；QoS2 本 Broker 暂不支持
        if (qos == 3) {
            throw new IllegalArgumentException("非法 QoS 值 3（malformed packet）");
        }
        if (qos == 2) {
            throw new IllegalArgumentException("本 Broker 暂不支持 QoS 2，拒绝该 PUBLISH");
        }
        // QoS 0 的 PUBLISH 其 DUP 必须为 0（MQTT-3.3.1-2）
        if (qos == 0 && msg.isDup()) {
            throw new IllegalArgumentException("QoS 0 报文 DUP 位必须为 0（malformed packet）");
        }
        msg.setQos(qos);

        // Topic
        if (payload.readableBytes() < 2) {
            throw new IllegalArgumentException("Topic长度字段需要2字节");
        }
        int topicLen = payload.readUnsignedShort();
        if (payload.readableBytes() < topicLen) {
            throw new IllegalArgumentException("Topic内容需要" + topicLen + "字节，剩余：" + payload.readableBytes());
        }
        byte[] topicBytes = new byte[topicLen];
        payload.readBytes(topicBytes);
        msg.setTopic(new String(topicBytes, StandardCharsets.UTF_8));

        // PacketId (QoS > 0 时)
        if (msg.getQos() > 0) {
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("PacketId需要2字节");
            }
            msg.setPacketId(payload.readUnsignedShort());
        }

        // Payload（剩余所有字节）
        byte[] data = new byte[payload.readableBytes()];
        payload.readBytes(data);
        msg.setPayload(data);

        log.info("PUBLISH 解析成功: topic={}, qos={}, payloadSize={}字节",
                msg.getTopic(), msg.getQos(), data.length);
        return msg;
    }

    /**
     * 解析 SUBSCRIBE 报文
     */
    private SubscribeMessage decodeSubscribe(ByteBuf payload) {
        SubscribeMessage msg = new SubscribeMessage();

        if (payload.readableBytes() < 2) {
            throw new IllegalArgumentException("SUBSCRIBE PacketId需要2字节");
        }
        msg.setPacketId(payload.readUnsignedShort());

        List<SubscribeMessage.Subscription> subs = new java.util.ArrayList<>();
        while (payload.readableBytes() > 0) {
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("TopicFilter长度字段需要2字节");
            }
            int topicLen = payload.readUnsignedShort();
            if (payload.readableBytes() < topicLen) {
                throw new IllegalArgumentException("TopicFilter内容需要" + topicLen + "字节");
            }
            byte[] topicBytes = new byte[topicLen];
            payload.readBytes(topicBytes);
            String topicFilter = new String(topicBytes, StandardCharsets.UTF_8);

            if (payload.readableBytes() < 1) {
                throw new IllegalArgumentException("QoS需要1字节");
            }
            int qos = payload.readUnsignedByte();

            SubscribeMessage.Subscription sub = new SubscribeMessage.Subscription();
            sub.setTopicFilter(topicFilter);
            sub.setMaxQos(qos);
            subs.add(sub);
        }

        msg.setSubscriptions(subs);
        log.info("SUBSCRIBE 解析成功: packetId={}, topics={}",
                msg.getPacketId(), subs.stream().map(s -> s.getTopicFilter() + "@QoS" + s.getMaxQos()).toList());
        return msg;
    }

    /**
     * 解析 UNSUBSCRIBE 报文
     */
    private UnsubscribeMessage decodeUnsubscribe(ByteBuf payload) {
        UnsubscribeMessage msg = new UnsubscribeMessage();

        if (payload.readableBytes() < 2) {
            throw new IllegalArgumentException("UNSUBSCRIBE PacketId需要2字节");
        }
        msg.setPacketId(payload.readUnsignedShort());

        List<String> topicFilters = new java.util.ArrayList<>();
        while (payload.readableBytes() > 0) {
            if (payload.readableBytes() < 2) {
                throw new IllegalArgumentException("TopicFilter长度字段需要2字节");
            }
            int topicLen = payload.readUnsignedShort();
            if (payload.readableBytes() < topicLen) {
                throw new IllegalArgumentException("TopicFilter内容需要" + topicLen + "字节");
            }
            byte[] topicBytes = new byte[topicLen];
            payload.readBytes(topicBytes);
            topicFilters.add(new String(topicBytes, StandardCharsets.UTF_8));
        }

        msg.setTopicFilters(topicFilters);
        log.info("UNSUBSCRIBE 解析成功: packetId={}, topics={}", msg.getPacketId(), topicFilters);
        return msg;
    }
}
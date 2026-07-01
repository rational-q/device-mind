package com.devicemind.broker.handler;

import com.devicemind.broker.config.BrokerConfig;
import com.devicemind.broker.kafka.forwarder.MessageForwarder;
import com.devicemind.broker.model.PublishMessage;
import com.devicemind.broker.service.MqttMessageStore;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * MQTT PUBLISH 消息处理器
 * <p>
 * 消息可靠性保证流程（QoS 1）：
 * <ol>
 *   <li>解析 PUBLISH 报文</li>
 *   <li>消息持久化到 Redis（先落盘）</li>
 *   <li>发送 PUBACK 给设备（确认收到）</li>
 *   <li>异步转发到 Kafka（补偿定时任务保证最终一致）</li>
 * </ol>
 * <p>
 * QoS 0：直接异步转发，不做持久化（允许偶发丢失）。
 * <p>
 * 安全：enforceProtocol 开启时，要求先 CONNECT，且只能向自身 deviceId 的主题发布。
 */
@Slf4j
public class PublishHandler extends SimpleChannelInboundHandler<PublishMessage> {

    private final MessageForwarder messageForwarder;
    private final MqttMessageStore messageStore;
    private final SessionManager sessionManager;
    private final BrokerConfig brokerConfig;

    public PublishHandler(MessageForwarder messageForwarder, MqttMessageStore messageStore,
                          SessionManager sessionManager, BrokerConfig brokerConfig) {
        super(false);
        this.messageForwarder = messageForwarder;
        this.messageStore = messageStore;
        this.sessionManager = sessionManager;
        this.brokerConfig = brokerConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PublishMessage msg) {
        TraceContext.set();
        try {
            boolean enforce = brokerConfig.getAuth().isEnforceProtocol();
            String authClientId = ctx.channel().attr(ConnectHandler.CLIENT_ID).get();

            // 必须先 CONNECT
            if (enforce && !Boolean.TRUE.equals(ctx.channel().attr(ConnectHandler.AUTHENTICATED).get())) {
                log.warn("未 CONNECT 就 PUBLISH，关闭连接: topic={}", msg.getTopic());
                ctx.close();
                return;
            }

            // 主题授权：设备只能向自身 deviceId 的主题发布，禁止伪造他人回执/数据
            String topicClientId = extractClientId(msg.getTopic());
            if (enforce && (authClientId == null || !authClientId.equals(topicClientId))) {
                log.warn("发布越权被拒: authClientId={}, topic={}", authClientId, msg.getTopic());
                ctx.close();
                return;
            }

            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
            log.info("收到设备数据: topic={}, qos={}, payloadLen={}", msg.getTopic(), msg.getQos(), payload.length());

            if (msg.getQos() == 1) {
                // 1. 先持久化到 Redis（确保不丢），用认证 clientId 优先，回退主题解析
                String clientId = authClientId != null ? authClientId : topicClientId;
                String messageId = messageStore.save(clientId, msg.getTopic(), payload);

                // 2. 立即发送 PUBACK（通知设备"我已收到"）
                ByteBuf pubAck = Unpooled.buffer(4);
                pubAck.writeByte(0x40);
                pubAck.writeByte(0x02);
                pubAck.writeShort(msg.getPacketId());
                ctx.writeAndFlush(pubAck);

                // 3. 异步转发到 Kafka
                messageForwarder.forwardWithStore(msg.getTopic(), payload, messageId);
            } else {
                // QoS 0：直接转发，允许偶发丢失
                messageForwarder.forward(msg.getTopic(), payload);
            }
        } catch (Exception e) {
            // 持久化失败：不发送 PUBACK，设备会重发
            log.error("处理 PUBLISH 消息异常（未 PUBACK，设备将重发）: topic={}", msg.getTopic(), e);
            // 不发送 PUBACK = 设备侧 Kafka client 会重试 PUBLISH
        } finally {
            TraceContext.clear();
        }
    }

    /**
     * 从 MQTT topic 中提取 clientId
     * 约定：topic 格式为 device/data/{clientId} 或 device/response/{clientId}
     */
    private String extractClientId(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }
}

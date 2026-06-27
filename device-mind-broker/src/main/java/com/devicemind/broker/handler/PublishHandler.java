package com.devicemind.broker.handler;

import com.devicemind.broker.model.PublishMessage;
import com.devicemind.broker.service.MessageForwarder;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class PublishHandler extends SimpleChannelInboundHandler<PublishMessage> {

    private final SessionManager sessionManager;
    private final MessageForwarder messageForwarder;

    public PublishHandler(SessionManager sessionManager, MessageForwarder messageForwarder) {
        super(false);
        this.sessionManager = sessionManager;
        this.messageForwarder = messageForwarder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PublishMessage msg) {
        TraceContext.set();
        try {
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
            log.info("收到设备数据: topic={}, payload={}", msg.getTopic(), payload);

            // QoS 1 回复 PUBACK
            if (msg.getQos() == 1) {
                ByteBuf pubAck = Unpooled.buffer(4);
                pubAck.writeByte(0x40);
                pubAck.writeByte(0x02);
                pubAck.writeShort(msg.getPacketId());
                ctx.writeAndFlush(pubAck);
            }

            // 转发到 Kafka → Core 消费
            messageForwarder.forward(payload);
        } finally {
            TraceContext.clear();
        }
    }
}

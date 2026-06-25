package com.devicemind.broker.handler;

import com.devicemind.broker.model.PublishMessage;
import com.devicemind.broker.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class PublishHandler extends SimpleChannelInboundHandler<PublishMessage> {

    private final SessionManager sessionManager;

    public PublishHandler(SessionManager sessionManager) {
        super(false);
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PublishMessage msg) {
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

        // TODO: 将数据转发给 core 服务（通过 HTTP 或消息队列）
        // 当前版本：仅打印日志，后续实现 MessageDispatcher 注入
    }
}

package com.devicemind.broker.handler;

import com.devicemind.broker.model.ConnectMessage;
import com.devicemind.broker.session.DeviceSession;
import com.devicemind.broker.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectHandler extends SimpleChannelInboundHandler<ConnectMessage> {
    private final SessionManager sessionManager;

    public ConnectHandler(SessionManager sessionManager) {
        super(false);
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ConnectMessage msg) {
        log.info("处理连接请求: clientId={}", msg.getClientId());

        DeviceSession session = new DeviceSession();
        session.setClientId(msg.getClientId());
        session.setChannel(ctx.channel());
        session.setConnectedAt(System.currentTimeMillis());
        session.setLastHeartbeatAt(System.currentTimeMillis());
        session.setKeepAlive(msg.getKeepAlive());
        sessionManager.register(session);

        // CONNACK 报文: 0x20 0x02 0x00 0x00
        ByteBuf connAck = Unpooled.buffer(4);
        connAck.writeByte(0x20);
        connAck.writeByte(0x02);
        connAck.writeByte(0x00); // Session Present = 0
        connAck.writeByte(0x00); // Return Code = 0 (接受)
        ctx.writeAndFlush(connAck);

        log.info("已回复 CONNACK 给设备: {}", msg.getClientId());
    }
}

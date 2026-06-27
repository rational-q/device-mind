package com.devicemind.broker.handler;

import com.devicemind.broker.model.ConnectMessage;
import com.devicemind.broker.service.DeviceAuthService;
import com.devicemind.broker.service.DeviceAuthService;
import com.devicemind.broker.session.DeviceSession;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectHandler extends SimpleChannelInboundHandler<ConnectMessage> {
    private final SessionManager sessionManager;
    private final DeviceAuthService deviceAuthService;

    public ConnectHandler(SessionManager sessionManager, DeviceAuthService deviceAuthService) {
        super(false);
        this.sessionManager = sessionManager;
        this.deviceAuthService = deviceAuthService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ConnectMessage msg) {
        TraceContext.set();
        try {
            String clientId = msg.getClientId();
            log.info("处理连接请求: clientId={}", clientId);

            // 设备认证
            if (!deviceAuthService.isRegistered(clientId)) {
                log.warn("未注册的设备尝试连接，拒绝: clientId={}", clientId);
                ByteBuf connNack = Unpooled.buffer(4);
                connNack.writeByte(0x20);
                connNack.writeByte(0x02);
                connNack.writeByte(0x00);
                connNack.writeByte(0x05); // 0x05 = 未授权
                ctx.writeAndFlush(connNack);
                ctx.close();
                return;
            }

            DeviceSession session = new DeviceSession();
            session.setClientId(msg.getClientId());
            session.setChannel(ctx.channel());
            session.setConnectedAt(System.currentTimeMillis());
            session.setLastHeartbeatAt(System.currentTimeMillis());
            session.setKeepAlive(msg.getKeepAlive());
            sessionManager.register(session);

            // 通知 Core 设备上线
            deviceAuthService.notifyStatusChange(clientId, "ONLINE");

            // CONNACK 报文: 0x20 0x02 0x00 0x00
            ByteBuf connAck = Unpooled.buffer(4);
            connAck.writeByte(0x20);
            connAck.writeByte(0x02);
            connAck.writeByte(0x00); // Session Present = 0
            connAck.writeByte(0x00); // Return Code = 0 (接受)
            ctx.writeAndFlush(connAck);

            log.info("已回复 CONNACK 给设备: {}", msg.getClientId());
        } finally {
            TraceContext.clear();
        }
    }
}

package com.devicemind.broker.handler;

import com.devicemind.broker.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisconnectHandler extends ChannelInboundHandlerAdapter {
    private final SessionManager sessionManager;

    public DisconnectHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("设备断开，清理会话: {}", ctx.channel().id().asShortText());
        sessionManager.unregister(ctx.channel());
        ctx.fireChannelInactive();
    }
}

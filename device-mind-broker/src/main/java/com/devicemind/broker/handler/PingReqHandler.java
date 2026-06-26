package com.devicemind.broker.handler;

import com.devicemind.broker.model.PingReqMessage;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingReqHandler extends SimpleChannelInboundHandler<PingReqMessage> {
    private final SessionManager sessionManager;

    public PingReqHandler(SessionManager sessionManager) {
        super(false);
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingReqMessage msg) {
        TraceContext.set();
        try {
            log.debug("收到心跳 PINGREQ");

            ByteBuf pingResp = Unpooled.buffer(2);
            pingResp.writeByte(0xD0);
            pingResp.writeByte(0x00);
            ctx.writeAndFlush(pingResp);

            sessionManager.updateHeartbeat(ctx.channel());
        } finally {
            TraceContext.clear();
        }
    }
}

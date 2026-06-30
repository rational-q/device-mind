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

    /** PINGRESP 报文（固定 2 字节，复用避免频繁分配） */
    private static final ByteBuf PINGRESP = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{(byte) 0xD0, 0x00}));

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
            ctx.writeAndFlush(PINGRESP.retainedDuplicate());
            sessionManager.updateHeartbeat(ctx.channel());
        } finally {
            TraceContext.clear();
        }
    }
}

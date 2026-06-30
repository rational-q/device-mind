package com.devicemind.broker.handler;

import com.devicemind.common.kafka.producer.DeviceStatusProducer;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.broker.session.SubscriptionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳超时处理器 — 超过心跳间隔未收到数据则断开连接
 */
@Slf4j
public class HeartbeatTimeoutHandler extends ChannelInboundHandlerAdapter {

    private final SessionManager sessionManager;
    private final SubscriptionManager subscriptionManager;
    private final DeviceStatusProducer deviceStatusProducer;

    public HeartbeatTimeoutHandler(SessionManager sessionManager, SubscriptionManager subscriptionManager,
                                   DeviceStatusProducer deviceStatusProducer) {
        this.sessionManager = sessionManager;
        this.subscriptionManager = subscriptionManager;
        this.deviceStatusProducer = deviceStatusProducer;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                String clientId = sessionManager.getClientId(ctx.channel());
                log.warn("心跳超时，断开设备: clientId={}", clientId);

                // 通知 Core 设备离线（Kafka）
                if (clientId != null) {
                    deviceStatusProducer.offline(clientId);
                }
                sessionManager.unregister(ctx.channel());
                subscriptionManager.removeChannel(ctx.channel());
                ctx.close();
                return;
            }
        }
        ctx.fireUserEventTriggered(evt);
    }
}

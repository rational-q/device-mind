package com.devicemind.broker.handler;

import com.devicemind.common.kafka.producer.DeviceStatusProducer;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.broker.session.SubscriptionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisconnectHandler extends ChannelInboundHandlerAdapter {
    private final SessionManager sessionManager;
    private final SubscriptionManager subscriptionManager;
    private final DeviceStatusProducer deviceStatusProducer;

    public DisconnectHandler(SessionManager sessionManager, SubscriptionManager subscriptionManager,
                             DeviceStatusProducer deviceStatusProducer) {
        this.sessionManager = sessionManager;
        this.subscriptionManager = subscriptionManager;
        this.deviceStatusProducer = deviceStatusProducer;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        TraceContext.set();
        try {
            String clientId = sessionManager.getClientId(ctx.channel());
            log.info("设备断开，清理会话和订阅: channelId={}, clientId={}",
                    ctx.channel().id().asShortText(), clientId);

            sessionManager.unregister(ctx.channel());
            subscriptionManager.removeChannel(ctx.channel());

            // 通知 Core 设备离线（Kafka）
            if (clientId != null) {
                deviceStatusProducer.offline(clientId);
            }

            ctx.fireChannelInactive();
        } finally {
            TraceContext.clear();
        }
    }
}

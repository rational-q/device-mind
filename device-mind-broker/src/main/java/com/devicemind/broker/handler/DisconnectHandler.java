package com.devicemind.broker.handler;

import com.devicemind.broker.service.DeviceAuthService;
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
    private final DeviceAuthService deviceAuthService;

    public DisconnectHandler(SessionManager sessionManager, SubscriptionManager subscriptionManager,
                             DeviceAuthService deviceAuthService) {
        this.sessionManager = sessionManager;
        this.subscriptionManager = subscriptionManager;
        this.deviceAuthService = deviceAuthService;
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

            // 通知 Core 设备离线
            if (clientId != null) {
                deviceAuthService.notifyStatusChange(clientId, "OFFLINE");
            }

            ctx.fireChannelInactive();
        } finally {
            TraceContext.clear();
        }
    }
}

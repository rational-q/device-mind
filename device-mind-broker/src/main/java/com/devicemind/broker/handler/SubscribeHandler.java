package com.devicemind.broker.handler;

import com.devicemind.broker.model.SubAckMessage;
import com.devicemind.broker.model.SubscribeMessage;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.broker.session.SessionStore;
import com.devicemind.broker.session.SubscriptionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT SUBSCRIBE 处理器
 * <p>
 * 处理订阅请求，支持：
 * <ul>
 *   <li>主题过滤 + QoS 协商（当前最高 QoS 1）</li>
 *   <li>订阅持久化到 Redis（Broker 重启后可恢复）</li>
 * </ul>
 */
@Slf4j
public class SubscribeHandler extends SimpleChannelInboundHandler<SubscribeMessage> {

    private final SubscriptionManager subscriptionManager;
    private final SessionManager sessionManager;
    private final SessionStore sessionStore;

    public SubscribeHandler(SubscriptionManager subscriptionManager,
                            SessionManager sessionManager,
                            SessionStore sessionStore) {
        super(false);
        this.subscriptionManager = subscriptionManager;
        this.sessionManager = sessionManager;
        this.sessionStore = sessionStore;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SubscribeMessage msg) {
        TraceContext.set();
        try {
            String clientId = sessionManager.getClientId(ctx.channel());
            log.info("收到订阅请求: clientId={}, packetId={}, topics={}",
                    clientId, msg.getPacketId(),
                    msg.getSubscriptions().stream().map(s -> s.getTopicFilter() + "@QoS" + s.getMaxQos()).toList());

            List<Integer> returnCodes = new ArrayList<>();
            for (SubscribeMessage.Subscription sub : msg.getSubscriptions()) {
                if (sub.getTopicFilter() == null || sub.getTopicFilter().isBlank()) {
                    returnCodes.add(0x80); // 失败
                    continue;
                }
                subscriptionManager.subscribe(ctx.channel(), sub.getTopicFilter());
                returnCodes.add(Math.min(sub.getMaxQos(), 1)); // 当前最高 QoS 1
            }

            // 持久化订阅到 Redis
            if (clientId != null) {
                try {
                    sessionStore.saveSubscriptions(clientId,
                            msg.getSubscriptions().stream()
                                    .map(SubscribeMessage.Subscription::getTopicFilter)
                                    .collect(java.util.stream.Collectors.toSet()));
                } catch (Exception e) {
                    log.warn("订阅持久化到 Redis 失败: clientId={}", clientId, e);
                }
            }

            ctx.writeAndFlush(new SubAckMessage(msg.getPacketId(), returnCodes));
            log.info("订阅完成: clientId={}, topics={}, returnCodes={}",
                    clientId,
                    msg.getSubscriptions().stream().map(s -> s.getTopicFilter()).toList(),
                    returnCodes);
        } finally {
            TraceContext.clear();
        }
    }
}

package com.devicemind.broker.handler;

import com.devicemind.broker.config.BrokerConfig;
import com.devicemind.broker.model.SubAckMessage;
import com.devicemind.broker.model.SubscribeMessage;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.broker.session.SessionStore;
import com.devicemind.broker.session.SubscriptionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.channel.ChannelFutureListener;
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
 *   <li>必须先 CONNECT（enforceProtocol 开启时）</li>
 *   <li>主题授权：设备只能订阅自己 deviceId 相关的主题（enforceProtocol 开启时）</li>
 *   <li>主题过滤 + QoS 协商（当前最高 QoS 1）</li>
 *   <li>订阅持久化到 Redis（Broker 重启后可恢复）</li>
 * </ul>
 */
@Slf4j
public class SubscribeHandler extends SimpleChannelInboundHandler<SubscribeMessage> {

    private final SubscriptionManager subscriptionManager;
    private final SessionManager sessionManager;
    private final SessionStore sessionStore;
    private final BrokerConfig brokerConfig;

    public SubscribeHandler(SubscriptionManager subscriptionManager,
                            SessionManager sessionManager,
                            SessionStore sessionStore,
                            BrokerConfig brokerConfig) {
        super(false);
        this.subscriptionManager = subscriptionManager;
        this.sessionManager = sessionManager;
        this.sessionStore = sessionStore;
        this.brokerConfig = brokerConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SubscribeMessage msg) {
        TraceContext.set();
        try {
            boolean enforce = brokerConfig.getAuth().isEnforceProtocol();

            // 必须先 CONNECT
            String authClientId = ctx.channel().attr(ConnectHandler.CLIENT_ID).get();
            if (enforce && !Boolean.TRUE.equals(ctx.channel().attr(ConnectHandler.AUTHENTICATED).get())) {
                log.warn("未 CONNECT 就 SUBSCRIBE，关闭连接");
                ctx.close();
                return;
            }

            String clientId = authClientId != null ? authClientId : sessionManager.getClientId(ctx.channel());
            log.info("收到订阅请求: clientId={}, packetId={}, topics={}",
                    clientId, msg.getPacketId(),
                    msg.getSubscriptions().stream().map(s -> s.getTopicFilter() + "@QoS" + s.getMaxQos()).toList());

            List<Integer> returnCodes = new ArrayList<>();
            for (SubscribeMessage.Subscription sub : msg.getSubscriptions()) {
                String filter = sub.getTopicFilter();
                if (filter == null || filter.isBlank()) {
                    returnCodes.add(0x80); // 失败
                    continue;
                }
                // 主题授权：只允许订阅与自身 deviceId 绑定的主题
                if (enforce && !isTopicAllowed(filter, authClientId)) {
                    log.warn("订阅越权被拒: clientId={}, filter={}", authClientId, filter);
                    returnCodes.add(0x80);
                    continue;
                }
                subscriptionManager.subscribe(ctx.channel(), filter);
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

    /**
     * 主题授权：设备只能订阅指向自己 deviceId 的主题。
     * <p>
     * 约定主题格式为 {@code device/{type}/{clientId}}（如 device/command/temp-001），
     * 第 3 段必须等于认证 clientId，禁止 {@code #} / {@code +} 通配符跨设备订阅。
     */
    private boolean isTopicAllowed(String filter, String clientId) {
        if (clientId == null) return false;
        if (filter.contains("#") || filter.contains("+")) return false; // 禁止通配符越权
        String[] parts = filter.split("/");
        return parts.length >= 3 && clientId.equals(parts[2]);
    }
}

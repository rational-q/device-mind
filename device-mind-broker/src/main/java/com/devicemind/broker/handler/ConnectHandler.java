package com.devicemind.broker.handler;

import com.devicemind.broker.config.BrokerConfig;
import com.devicemind.broker.model.ConnAckMessage;
import com.devicemind.broker.model.ConnectMessage;
import com.devicemind.broker.service.DeviceAuthService;
import com.devicemind.common.kafka.producer.DeviceStatusProducer;
import com.devicemind.broker.session.DeviceSession;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * MQTT CONNECT 处理器
 * <p>
 * 处理设备连接请求，支持：
 * <ul>
 *   <li>协议版本校验（MQTT 3.1.1，level=4）</li>
 *   <li>clientId 非空校验</li>
 *   <li>用户名密码认证</li>
 *   <li>设备注册校验</li>
 *   <li>连接成功后在 channel 上打「已认证」标记（供后续 PUBLISH/SUBSCRIBE 校验）</li>
 *   <li>按 1.5×keepAlive 动态设置心跳超时</li>
 *   <li>Session 恢复 + 持久化到 Redis</li>
 * </ul>
 */
@Slf4j
public class ConnectHandler extends SimpleChannelInboundHandler<ConnectMessage> {

    /** channel 属性：是否已完成 CONNECT 认证 */
    public static final AttributeKey<Boolean> AUTHENTICATED = AttributeKey.valueOf("mqtt.authenticated");
    /** channel 属性：认证通过的 clientId（用于 topic 授权，避免信任 topic 里的 clientId） */
    public static final AttributeKey<String> CLIENT_ID = AttributeKey.valueOf("mqtt.clientId");

    private final SessionManager sessionManager;
    private final DeviceAuthService deviceAuthService;
    private final BrokerConfig brokerConfig;
    private final DeviceStatusProducer deviceStatusProducer;

    public ConnectHandler(SessionManager sessionManager, DeviceAuthService deviceAuthService,
                          BrokerConfig brokerConfig, DeviceStatusProducer deviceStatusProducer) {
        super(false);
        this.sessionManager = sessionManager;
        this.deviceAuthService = deviceAuthService;
        this.brokerConfig = brokerConfig;
        this.deviceStatusProducer = deviceStatusProducer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ConnectMessage msg) {
        TraceContext.set();
        try {
            String clientId = msg.getClientId();
            log.info("处理连接请求: clientId={}, cleanSession={}", clientId, msg.isCleanSession());

            // MQTT 3.1.1 协议校验
            if (!"MQTT".equals(msg.getProtocolName()) || msg.getProtocolLevel() != 4) {
                log.warn("不支持的协议版本，拒绝: clientId={}, protocol={}, level={}",
                        clientId, msg.getProtocolName(), msg.getProtocolLevel());
                ctx.writeAndFlush(new ConnAckMessage(0x01, false))
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            // clientId 非空校验（MQTT-3.1.3-8：空 clientId 拒绝，返回 0x02）
            if (clientId == null || clientId.isBlank()) {
                log.warn("空 clientId，拒绝连接");
                ctx.writeAndFlush(new ConnAckMessage(0x02, false))
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            // 用户名密码认证
            if (brokerConfig.getAuth().isEnabled()) {
                String expectedUser = brokerConfig.getAuth().getUsername();
                String expectedPass = brokerConfig.getAuth().getPassword();
                if (expectedUser != null && !expectedUser.isBlank()) {
                    if (!expectedUser.equals(msg.getUsername())
                            || !expectedPass.equals(msg.getPassword())) {
                        log.warn("认证失败，拒绝: clientId={}", clientId);
                        ctx.writeAndFlush(new ConnAckMessage(0x04, false))
                                .addListener(ChannelFutureListener.CLOSE);
                        return;
                    }
                }
            }

            // 设备注册校验
            if (!deviceAuthService.isRegistered(clientId)) {
                log.warn("未注册的设备尝试连接，拒绝: clientId={}", clientId);
                ctx.writeAndFlush(new ConnAckMessage(0x05, false))
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            // 标记该 channel 已认证 + 记录认证 clientId（供 PUBLISH/SUBSCRIBE 授权校验）
            ctx.channel().attr(AUTHENTICATED).set(Boolean.TRUE);
            ctx.channel().attr(CLIENT_ID).set(clientId);

            // 按 1.5×keepAlive 动态设置心跳超时（keepAlive=0 表示不启用，用默认值）
            adjustIdleTimeout(ctx, msg.getKeepAlive());

            // 判断是否恢复旧会话
            boolean sessionPresent = !msg.isCleanSession() && sessionManager.hasPersistedSession(clientId);

            DeviceSession session = new DeviceSession();
            session.setClientId(msg.getClientId());
            session.setChannel(ctx.channel());
            session.setConnectedAt(System.currentTimeMillis());
            session.setLastHeartbeatAt(System.currentTimeMillis());
            session.setKeepAlive(msg.getKeepAlive());
            sessionManager.register(session);

            // 如果恢复了旧会话，尝试恢复订阅
            if (sessionPresent) {
                sessionManager.restoreSubscriptions(clientId);
                log.info("恢复设备旧会话: clientId={}", clientId);
            }

            // 通知 Core 设备上线（Kafka）
            deviceStatusProducer.online(clientId);

            // CONNACK — 接受连接，携带 sessionPresent 标志
            ctx.writeAndFlush(new ConnAckMessage(0x00, sessionPresent));

            log.info("已回复 CONNACK 给设备: clientId={}, sessionPresent={}", clientId, sessionPresent);
        } finally {
            TraceContext.clear();
        }
    }

    /**
     * 按 MQTT 3.1.1 建议以 1.5×keepAlive 作为读超时，替换初始 IdleStateHandler。
     */
    private void adjustIdleTimeout(ChannelHandlerContext ctx, int keepAlive) {
        if (keepAlive <= 0) return; // 0 = 客户端不启用 keepAlive，保留默认超时
        int timeout = (int) Math.ceil(keepAlive * 1.5);
        try {
            if (ctx.pipeline().get("idleStateHandler") != null) {
                ctx.pipeline().replace("idleStateHandler", "idleStateHandler",
                        new IdleStateHandler(timeout, 0, 0, TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            log.warn("动态调整心跳超时失败，沿用默认: keepAlive={}", keepAlive, e);
        }
    }
}

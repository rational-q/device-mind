package com.devicemind.broker;

import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.broker.codec.MqttDecoder;
import com.devicemind.broker.codec.MqttEncoder;
import com.devicemind.broker.config.BrokerConfig;
import com.devicemind.broker.handler.ConnectHandler;
import com.devicemind.broker.handler.DisconnectHandler;
import com.devicemind.broker.handler.ExceptionHandler;
import com.devicemind.broker.handler.HeartbeatTimeoutHandler;
import com.devicemind.broker.handler.PingReqHandler;
import com.devicemind.broker.handler.PublishHandler;
import com.devicemind.broker.handler.SubscribeHandler;
import com.devicemind.broker.handler.UnsubscribeHandler;
import com.devicemind.broker.service.DeviceAuthService;
import com.devicemind.common.kafka.producer.DeviceStatusProducer;
import com.devicemind.broker.kafka.forwarder.MessageForwarder;
import com.devicemind.broker.service.MqttMessageStore;
import com.devicemind.broker.session.SessionManager;
import com.devicemind.broker.session.SessionStore;
import com.devicemind.broker.session.SubscriptionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BrokerServer {

    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private DeviceAuthService deviceAuthService;
    @Autowired
    private BrokerConfig brokerConfig;
    @Autowired
    private MessageForwarder messageForwarder;
    @Autowired
    private MqttMessageStore mqttMessageStore;
    @Autowired
    private DeviceStatusProducer deviceStatusProducer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        int bossThreads = brokerConfig.getBossThreads();
        int workerThreads = Runtime.getRuntime().availableProcessors() * brokerConfig.getWorkerThreadsMultiplier();

        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, brokerConfig.getBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 初始 IdleStateHandler 用配置的默认超时；CONNECT 收到 keepAlive 后
                            // ConnectHandler 会按 1.5×keepAlive 动态替换（见 ConnectHandler）
                            pipeline.addLast("idleStateHandler",
                                    new IdleStateHandler(brokerConfig.getHeartbeatTimeout(), 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new HeartbeatTimeoutHandler(sessionManager, subscriptionManager, deviceStatusProducer));
                            pipeline.addLast(new MqttDecoder(brokerConfig.getMaxRemainingLength()));
                            pipeline.addLast(new MqttEncoder());
                            pipeline.addLast(new ConnectHandler(sessionManager, deviceAuthService, brokerConfig, deviceStatusProducer));
                            pipeline.addLast(new SubscribeHandler(subscriptionManager, sessionManager, sessionStore, brokerConfig));
                            pipeline.addLast(new UnsubscribeHandler(subscriptionManager));
                            pipeline.addLast(new PublishHandler(messageForwarder, mqttMessageStore, sessionManager, brokerConfig));
                            pipeline.addLast(new PingReqHandler(sessionManager));
                            pipeline.addLast(new DisconnectHandler(sessionManager, subscriptionManager, deviceStatusProducer));
                            pipeline.addLast(new ExceptionHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(brokerConfig.getMqttPort()).sync();
            serverChannel = future.channel();
            log.info("MQTT Broker 启动成功，监听端口: {}", brokerConfig.getMqttPort());
        } catch (Exception e) {
            log.error("MQTT Broker 启动失败", e);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw new RuntimeException("MQTT Broker 启动失败", e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("MQTT Broker 正在关闭...");

        // 1. 关闭 MQTT 服务端口（停止接收新连接）
        if (serverChannel != null) {
            serverChannel.close();
        }

        // 2. 逐一清理在线会话的 Redis 持久化（避免僵尸 session）
        try {
            for (var entry : sessionManager.getAllSessions().entrySet()) {
                sessionStore.remove(entry.getKey());
            }
            log.info("已清理 {} 个 Redis 会话", sessionManager.getOnlineCount());
        } catch (Exception e) {
            log.warn("Redis 会话清理异常", e);
        }

        // 3. 优雅关闭 Netty 线程池
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();

        log.info("MQTT Broker 已关闭");
    }
}

package com.devicemind.broker;

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
import com.devicemind.broker.service.MessageForwarder;
import com.devicemind.broker.session.SessionManager;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerServer {

    private final SessionManager sessionManager;
    private final SubscriptionManager subscriptionManager;
    private final DeviceAuthService deviceAuthService;
    private final BrokerConfig brokerConfig;
    private final MessageForwarder messageForwarder;

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
                            pipeline.addLast(new IdleStateHandler(brokerConfig.getHeartbeatTimeout(), 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new HeartbeatTimeoutHandler(sessionManager, subscriptionManager));
                            pipeline.addLast(new MqttDecoder());
                            pipeline.addLast(new MqttEncoder());
                            pipeline.addLast(new ConnectHandler(sessionManager, deviceAuthService));
                            pipeline.addLast(new SubscribeHandler(subscriptionManager));
                            pipeline.addLast(new UnsubscribeHandler(subscriptionManager));
                            pipeline.addLast(new PublishHandler(sessionManager, messageForwarder));
                            pipeline.addLast(new PingReqHandler(sessionManager));
                            pipeline.addLast(new DisconnectHandler(sessionManager, subscriptionManager, deviceAuthService));
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
        }
    }

    @PreDestroy
    public void stop() {
        log.info("MQTT Broker 正在关闭...");
        if (serverChannel != null) serverChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("MQTT Broker 已关闭");
    }
}

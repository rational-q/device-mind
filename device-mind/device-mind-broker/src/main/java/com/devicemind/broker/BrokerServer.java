package com.devicemind.broker;

import com.devicemind.broker.codec.MqttDecoder;
import com.devicemind.broker.codec.MqttEncoder;
import com.devicemind.broker.handler.ConnectHandler;
import com.devicemind.broker.handler.DisconnectHandler;
import com.devicemind.broker.handler.ExceptionHandler;
import com.devicemind.broker.handler.PingReqHandler;
import com.devicemind.broker.handler.PublishHandler;
import com.devicemind.broker.session.SessionManager;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerServer {
    private final SessionManager sessionManager;

    @Value("${broker.mqtt-port:1883}")
    private int mqttPort;

    @Value("${broker.heartbeat-timeout:120}")
    private int heartbeatTimeout;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new IdleStateHandler(heartbeatTimeout, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new MqttDecoder());
                            pipeline.addLast(new MqttEncoder());
                            pipeline.addLast(new ConnectHandler(sessionManager));
                            pipeline.addLast(new PublishHandler(sessionManager));
                            pipeline.addLast(new PingReqHandler(sessionManager));
                            pipeline.addLast(new DisconnectHandler(sessionManager));
                            pipeline.addLast(new ExceptionHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(mqttPort).sync();
            serverChannel = future.channel();
            log.info("MQTT Broker 启动成功，监听端口: {}", mqttPort);
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

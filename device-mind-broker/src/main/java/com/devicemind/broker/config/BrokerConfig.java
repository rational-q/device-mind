package com.devicemind.broker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT Broker 配置属性
 * <p>
 * 对应 application.yml 中 broker.* 前缀
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "broker")
public class BrokerConfig {

    /** Netty MQTT 监听端口 */
    private int mqttPort = 1883;

    /** 心跳超时时间（秒） */
    private int heartbeatTimeout = 120;

    /** Boss 线程数（默认1，负责接受连接） */
    private int bossThreads = 1;

    /** Worker 线程数倍数（相对于 CPU 核数） */
    private int workerThreadsMultiplier = 2;

    /** TCP 连接 backlog 队列大小 */
    private int backlog = 1024;
}

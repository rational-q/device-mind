package com.devicemind.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式客户端配置
 * <p>
 * 基于 spring.data.redis 的连接参数自动配置 RedissonClient，
 * 提供分布式锁（RLock）、分布式集合（RMap/RSet/RList）等能力。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>分布式幂等锁 — {@code RLock} 替代单机 Caffeine 缓存</li>
 *   <li>MQTT Session 存储 — {@code RMap} 替代手写 Hash 操作</li>
 *   <li>Kafka 补偿任务互斥 — 多 broker 实例下只有一个执行补偿</li>
 *   <li>设备在线状态缓存 — 跨 broker 节点共享</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout:3000}")
    private int timeout;

    @Value("${redisson.connection-pool-size:32}")
    private int connectionPoolSize;

    @Value("${redisson.connection-minimum-idle-size:8}")
    private int connectionMinimumIdleSize;

    @Value("${redisson.retry-attempts:3}")
    private int retryAttempts;

    @Value("${redisson.retry-interval:1000}")
    private int retryInterval;

    @Value("${redisson.dns-monitoring-interval:5000}")
    private long dnsMonitoringInterval;

    /**
     * RedissonClient — 核心客户端
     * <p>
     * 配置要点：
     * <ul>
     *   <li>JSON 序列化（JsonJacksonCodec）— 可直接存取 Java 对象</li>
     *   <li>连接池大小 — 匹配并发量</li>
     *   <li>重试机制 — 网络抖动时自动重连</li>
     * </ul>
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RedissonClient redissonClient() {
        Config config = new Config();

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setTimeout(timeout)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setDnsMonitoringInterval(dnsMonitoringInterval);

        // 有密码时设置
        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }

        // 使用 JSON 序列化，方便直接存取 Java 对象
        config.setCodec(new JsonJacksonCodec());

        // 线程池配置
        config.setThreads(Runtime.getRuntime().availableProcessors() * 2);
        config.setNettyThreads(Runtime.getRuntime().availableProcessors() * 2);

        return Redisson.create(config);
    }
}

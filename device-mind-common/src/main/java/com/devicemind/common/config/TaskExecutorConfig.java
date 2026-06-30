package com.devicemind.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 业务线程池配置
 * <p>
 * 两个线程池：
 * <ul>
 *   <li>{@code kafkaConsumerExecutor} — Kafka Consumer 消息处理专用。有界队列 + CallerRunsPolicy，
 *   线程池饱和时由 Kafka 监听线程直接执行，天然背压。单条超时可配，超时抛异常触发 ErrorHandler 重试。</li>
 *   <li>{@code mainThreadPoolTaskExecutor} — 通用异步任务池（保留兼容旧代码）。</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class TaskExecutorConfig {

    public static final String KAFKA_CONSUMER_EXECUTOR = "kafkaConsumerExecutor";
    public static final String MAIN_EXECUTOR = "mainThreadPoolTaskExecutor";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    // ===== Kafka Consumer 线程池参数 =====

    @Value("${consumer.executor.core-pool-size:8}")
    private int kafkaCorePoolSize;

    @Value("${consumer.executor.max-pool-size:16}")
    private int kafkaMaxPoolSize;

    @Value("${consumer.executor.queue-capacity:200}")
    private int kafkaQueueCapacity;

    @Value("${consumer.executor.keep-alive-seconds:60}")
    private int kafkaKeepAliveSeconds;

    // ===== Kafka Consumer 专用线程池 =====

    /**
     * Kafka 消息处理线程池
     * <p>
     * 每个 Consumer 将业务处理逻辑提交到此线程池，等待完成后再 ack。
     * 设计要点：
     * <ul>
     *   <li>有界队列 200 — 防止 OOM</li>
     *   <li>CallerRunsPolicy — 队列满时由 Kafka 监听线程直接执行，形成背压</li>
     *   <li>核心线程数 8 — 应对稳态，最大 16 — 应对峰值</li>
     * </ul>
     */
    @Bean(KAFKA_CONSUMER_EXECUTOR)
    public ThreadPoolTaskExecutor kafkaConsumerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(kafkaCorePoolSize);
        executor.setMaxPoolSize(kafkaMaxPoolSize);
        executor.setQueueCapacity(kafkaQueueCapacity);
        executor.setKeepAliveSeconds(kafkaKeepAliveSeconds);
        executor.setThreadNamePrefix("kafka-consume-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("Kafka Consumer 线程池已创建: core={}, max={}, queue={}",
                kafkaCorePoolSize, kafkaMaxPoolSize, kafkaQueueCapacity);
        return executor;
    }

    // ===== 通用线程池（保留兼容旧代码） =====

    @Bean(MAIN_EXECUTOR)
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        log.info("创建通用线程池: cpu={}", CPU_COUNT);
        executor.setCorePoolSize(CPU_COUNT * 2);
        executor.setMaxPoolSize(CPU_COUNT * 4);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("DEVICE-THREAD-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.afterPropertiesSet();
        executor.initialize();
        return executor;
    }
}

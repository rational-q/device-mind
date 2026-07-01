package com.devicemind.broker.kafka.compensation;

import com.devicemind.broker.kafka.forwarder.MessageForwarder;
import com.devicemind.broker.service.MqttMessageStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 发送补偿定时任务
 * <p>
 * 定期扫描 MqttMessageStore 中 FAILED 状态的消息，重新尝试发送到 Kafka。
 * <p>
 * 三档策略：
 * <ul>
 *   <li>首次失败 → 进入 FAILED_SET，等 10s 后的补偿扫描</li>
 *   <li>1-3 次重试 → 每次都重试发送</li>
 *   <li>4-9 次重试 → 仍然重试，但日志升级到 WARN</li>
 *   <li>≥10 次重试 → 标记 DEAD，触发告警</li>
 * </ul>
 */
@Slf4j
@Component
public class KafkaCompensationScheduler {

    @Autowired
    private MqttMessageStore messageStore;
    @Autowired
    private MessageForwarder messageForwarder;

    /**
     * 每 10 秒扫描 FAILED 消息并重试
     */
    @Scheduled(fixedDelay = 10_000)
    public void compensateFailedMessages() {
        var failed = messageStore.getFailedMessages(Duration.ofMinutes(30));
        if (failed.isEmpty()) return;

        log.info("Kafka 补偿任务: 待重试消息 {} 条", failed.size());

        for (var msg : failed) {
            // 超过最大存活时间
            if (System.currentTimeMillis() - msg.createdAt() > Duration.ofMinutes(30).toMillis()) {
                messageStore.markDead(msg.messageId());
                log.error("消息超出最大存活时间，标记死信: messageId={}, topic={}",
                        msg.messageId(), msg.mqttTopic());
                continue;
            }

            // 重试次数超限
            if (msg.retryCount() >= 10) {
                messageStore.markDead(msg.messageId());
                log.error("消息重试耗尽({}次)，标记死信: messageId={}, topic={}",
                        msg.retryCount(), msg.messageId(), msg.mqttTopic());
                continue;
            }

            // 复用 MessageForwarder 的路由逻辑，保证补偿重试与首次转发路由一致
            CompletableFuture<?> future = messageForwarder.sendByTopic(msg.mqttTopic(), msg.payload());

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    messageStore.markDelivered(msg.messageId());
                    log.debug("补偿重试成功: messageId={}, retryCount={}",
                            msg.messageId(), msg.retryCount());
                } else {
                    int newCount = messageStore.incrementRetry(msg.messageId());
                    if (newCount >= 10) {
                        log.error("补偿重试耗尽，消息进死信: messageId={}, {}", msg.messageId(), ex.getMessage());
                    } else if (newCount >= 4) {
                        log.warn("补偿重试失败({}/10): messageId={}, {}", newCount, msg.messageId(), ex.getMessage());
                    } else {
                        log.info("补偿重试失败({}/10): messageId={}", newCount, msg.messageId());
                    }
                }
            });
        }
    }

    /**
     * 每 5 分钟清理已投递的过期消息
     */
    @Scheduled(fixedDelay = 300_000)
    public void cleanDeliveredMessages() {
        messageStore.cleanDelivered();
    }

    /**
     * 每分钟上报消息存储健康状态（日志 + 后续可接 Prometheus）
     */
    @Scheduled(fixedDelay = 60_000)
    public void reportStoreHealth() {
        long deadCount = messageStore.getDeadMessageCount();
        long failedCount = messageStore.getFailedMessageCount();
        if (deadCount > 0 || failedCount > 0) {
            log.warn("消息存储健康检查: dead={}, failed={}", deadCount, failedCount);
        }
    }
}

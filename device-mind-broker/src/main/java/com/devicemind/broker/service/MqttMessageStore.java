package com.devicemind.broker.service;

import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 消息持久化存储（基于 Redis）
 * <p>
 * 在 MQTT → Kafka 转发链路中提供"先持久化再 PUBACK → 异步发送 Kafka → 确认清理"的可靠性保证。
 * <p>
 * Redis 数据结构：
 * <ul>
 *   <li>{@code msg:store:{messageId}} — Hash: topic, payload, status, retryCount, createdAt</li>
 *   <li>{@code msg:failed:set} — Set: 所有 FAILED 状态的消息 ID（补偿扫描用）</li>
 *   <li>{@code msg:inflight:{clientId}} — List: 客户端级别的 inflight 消息 ID</li>
 * </ul>
 */
@Slf4j
@Service
public class MqttMessageStore {

    private static final String KEY_PREFIX = "msg:store:";
    private static final String FAILED_SET_KEY = "msg:failed:set";
    private static final String INFLIGHT_PREFIX = "msg:inflight:";
    private static final Duration MSG_TTL = Duration.ofHours(1);

    @Autowired
    private StringRedisTemplate redisTemplate;
    /**
     * 消息状态
     */
    public enum Status {
        /** 已存储，等待异步发送 Kafka */
        ACK_PENDING,
        /** Kafka 发送成功 */
        DELIVERED,
        /** Kafka 发送失败，等待补偿重试 */
        FAILED,
        /** 重试耗尽，进入死信 */
        DEAD
    }

    /**
     * 存储的消息实体
     */
    public record StoredMessage(
            String messageId,
            String mqttTopic,
            String payload,
            Status status,
            int retryCount,
            long createdAt
    ) {}

    /**
     * 持久化消息（在 PUBACK 之前调用）
     *
     * @param clientId  设备 ID
     * @param mqttTopic MQTT 主题
     * @param payload   消息体
     * @return 消息 ID
     */
    public String save(String clientId, String mqttTopic, String payload) {
        String messageId = UUID.randomUUID().toString();
        String key = KEY_PREFIX + messageId;

        redisTemplate.opsForHash().put(key, "clientId", clientId);
        redisTemplate.opsForHash().put(key, "mqttTopic", mqttTopic);
        redisTemplate.opsForHash().put(key, "payload", payload);
        redisTemplate.opsForHash().put(key, "status", Status.ACK_PENDING.name());
        redisTemplate.opsForHash().put(key, "retryCount", "0");
        redisTemplate.opsForHash().put(key, "createdAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(key, MSG_TTL);

        // 加入 inflight 列表
        redisTemplate.opsForList().rightPush(INFLIGHT_PREFIX + clientId, messageId);
        redisTemplate.expire(INFLIGHT_PREFIX + clientId, MSG_TTL);

        log.debug("消息已持久化: messageId={}, clientId={}, topic={}", messageId, clientId, mqttTopic);
        return messageId;
    }

    /**
     * 标记消息已成功投递到 Kafka
     */
    public void markDelivered(String messageId) {
        String key = KEY_PREFIX + messageId;
        redisTemplate.opsForHash().put(key, "status", Status.DELIVERED.name());
        redisTemplate.opsForSet().remove(FAILED_SET_KEY, messageId);
        redisTemplate.expire(key, Duration.ofMinutes(5)); // 缩短 TTL，快速清理
        log.debug("消息已投递: messageId={}", messageId);
    }

    /**
     * 标记消息发送失败（等待补偿）
     */
    public void markFailed(String messageId) {
        String key = KEY_PREFIX + messageId;
        Object retryObj = redisTemplate.opsForHash().get(key, "retryCount");
        int retryCount = retryObj != null ? Integer.parseInt(retryObj.toString()) : 0;
        redisTemplate.opsForHash().put(key, "status", Status.FAILED.name());
        redisTemplate.opsForHash().put(key, "retryCount", String.valueOf(retryCount + 1));
        redisTemplate.opsForSet().add(FAILED_SET_KEY, messageId);
        log.warn("消息发送失败，进入补偿队列: messageId={}, retryCount={}", messageId, retryCount + 1);
    }

    /**
     * 递增重试计数
     */
    public int incrementRetry(String messageId) {
        String key = KEY_PREFIX + messageId;
        Long retryCount = redisTemplate.opsForHash().increment(key, "retryCount", 1);
        int count = retryCount != null ? retryCount.intValue() : 0;
        if (count >= 10) {
            redisTemplate.opsForHash().put(key, "status", Status.DEAD.name());
            redisTemplate.opsForSet().remove(FAILED_SET_KEY, messageId);
            log.error("消息重试耗尽，进入死信: messageId={}, retryCount={}", messageId, count);
        }
        return count;
    }

    /**
     * 标记消息为死信
     */
    public void markDead(String messageId) {
        String key = KEY_PREFIX + messageId;
        redisTemplate.opsForHash().put(key, "status", Status.DEAD.name());
        redisTemplate.opsForSet().remove(FAILED_SET_KEY, messageId);
    }

    /**
     * 获取需要补偿的失败消息列表
     *
     * @param maxAge 最大存活时间（超过此时间的消息不再补偿）
     * @return 失败消息列表
     */
    public List<StoredMessage> getFailedMessages(Duration maxAge) {
        Set<String> failedIds = redisTemplate.opsForSet().members(FAILED_SET_KEY);
        if (failedIds == null || failedIds.isEmpty()) {
            return List.of();
        }

        long cutoffTime = System.currentTimeMillis() - maxAge.toMillis();
        List<StoredMessage> result = new ArrayList<>();

        for (String messageId : failedIds) {
            try {
                StoredMessage msg = getMessage(messageId);
                if (msg != null && msg.createdAt() > cutoffTime) {
                    result.add(msg);
                } else if (msg != null) {
                    // 超时消息标记为死信
                    markDead(messageId);
                    log.warn("失败消息超过最大存活时间，标记为死信: messageId={}", messageId);
                }
            } catch (Exception e) {
                log.warn("读取失败消息异常: messageId={}", messageId, e);
            }
        }
        return result;
    }

    /**
     * 根据消息 ID 获取消息详情
     */
    public StoredMessage getMessage(String messageId) {
        String key = KEY_PREFIX + messageId;
        String mqttTopic = (String) redisTemplate.opsForHash().get(key, "mqttTopic");
        if (mqttTopic == null) {
            return null; // 已过期或不存在
        }

        String payload = (String) redisTemplate.opsForHash().get(key, "payload");
        String statusStr = (String) redisTemplate.opsForHash().get(key, "status");
        String retryStr = (String) redisTemplate.opsForHash().get(key, "retryCount");
        String createdAtStr = (String) redisTemplate.opsForHash().get(key, "createdAt");

        Status status = statusStr != null ? Status.valueOf(statusStr) : Status.FAILED;
        int retryCount = retryStr != null ? Integer.parseInt(retryStr) : 0;
        long createdAt = createdAtStr != null ? Long.parseLong(createdAtStr) : System.currentTimeMillis();

        return new StoredMessage(messageId, mqttTopic, payload, status, retryCount, createdAt);
    }

    /**
     * 获取客户端的所有 inflight 消息 ID
     */
    public List<String> getInflightMessageIds(String clientId) {
        List<String> ids = redisTemplate.opsForList().range(INFLIGHT_PREFIX + clientId, 0, -1);
        return ids != null ? ids : List.of();
    }

    /**
     * 从 inflight 列表移除指定消息
     */
    public void removeFromInflight(String clientId, String messageId) {
        redisTemplate.opsForList().remove(INFLIGHT_PREFIX + clientId, 0, messageId);
    }

    /**
     * 获取死信消息数量（监控用）
     */
    public long getDeadMessageCount() {
        // 扫描所有 msg:store:* key 统计 DEAD 状态
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return 0;
        return keys.stream()
                .filter(k -> Status.DEAD.name().equals(redisTemplate.opsForHash().get(k, "status")))
                .count();
    }

    /**
     * 获取失败消息数量（监控用）
     */
    public long getFailedMessageCount() {
        Long size = redisTemplate.opsForSet().size(FAILED_SET_KEY);
        return size != null ? size : 0;
    }

    /**
     * 清理已投递的消息（定时任务调用）
     */
    public void cleanDelivered() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;

        int cleaned = 0;
        for (String key : keys) {
            String status = (String) redisTemplate.opsForHash().get(key, "status");
            if (Status.DELIVERED.name().equals(status)) {
                redisTemplate.delete(key);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.debug("清理已投递消息: {} 条", cleaned);
        }
    }

    /**
     * 通过 payload 中的 idempotencyKey 去重
     */
    public boolean isDuplicate(String idempotencyKey) {
        String dedupKey = "msg:dedup:" + idempotencyKey;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofMinutes(30));
        return success == null || !success;
    }
}

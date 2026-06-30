package com.devicemind.broker.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MqttMessageStore {

    private static final String KEY_PREFIX = "msg:store:";
    private static final String FAILED_SET = "msg:failed:set";
    private static final String INFLIGHT_PREFIX = "msg:inflight:";
    private static final String DEDUP_PREFIX = "msg:dedup:";
    private static final Duration MSG_TTL = Duration.ofHours(1);

    @Autowired
    private RedissonClient redisson;

    public enum Status { ACK_PENDING, DELIVERED, FAILED, DEAD }

    public record StoredMessage(String messageId, String clientId, String mqttTopic,
                                 String payload, Status status, int retryCount, long createdAt) {}

    public String save(String clientId, String mqttTopic, String payload) {
        String messageId = UUID.randomUUID().toString();
        RMap<String, String> map = redisson.getMap(KEY_PREFIX + messageId);
        map.put("clientId", clientId);
        map.put("mqttTopic", mqttTopic);
        map.put("payload", payload);
        map.put("status", Status.ACK_PENDING.name());
        map.put("retryCount", "0");
        map.put("createdAt", String.valueOf(System.currentTimeMillis()));
        map.expire(MSG_TTL);

        redisson.getList(INFLIGHT_PREFIX + clientId).add(messageId);
        redisson.getList(INFLIGHT_PREFIX + clientId).expire(MSG_TTL);
        log.debug("消息已持久化: messageId={}", messageId);
        return messageId;
    }

    public void markDelivered(String messageId) {
        RMap<String, String> map = redisson.getMap(KEY_PREFIX + messageId);
        map.put("status", Status.DELIVERED.name());
        redisson.getSet(FAILED_SET).remove(messageId);
        map.expire(Duration.ofMinutes(5));
    }

    public void markFailed(String messageId) {
        RMap<String, String> map = redisson.getMap(KEY_PREFIX + messageId);
        int retry = Integer.parseInt(map.getOrDefault("retryCount", "0")) + 1;
        map.put("status", Status.FAILED.name());
        map.put("retryCount", String.valueOf(retry));
        redisson.getSet(FAILED_SET).add(messageId);
    }

    public int incrementRetry(String messageId) {
        RMap<String, String> map = redisson.getMap(KEY_PREFIX + messageId);
        int count = Integer.parseInt(map.getOrDefault("retryCount", "0")) + 1;
        map.put("retryCount", String.valueOf(count));
        if (count >= 10) {
            map.put("status", Status.DEAD.name());
            redisson.getSet(FAILED_SET).remove(messageId);
            log.error("消息重试耗尽，进死信: {}", messageId);
        }
        return count;
    }

    public void markDead(String messageId) {
        redisson.getMap(KEY_PREFIX + messageId).put("status", Status.DEAD.name());
        redisson.getSet(FAILED_SET).remove(messageId);
    }

    public List<StoredMessage> getFailedMessages(Duration maxAge) {
        RSet<String> failedSet = redisson.getSet(FAILED_SET);
        if (failedSet.isEmpty()) return List.of();

        long cutoff = System.currentTimeMillis() - maxAge.toMillis();
        List<StoredMessage> result = new ArrayList<>();
        for (String mid : failedSet) {
            var msg = getMessage(mid);
            if (msg != null && msg.createdAt() > cutoff) result.add(msg);
            else if (msg != null) markDead(mid);
        }
        return result;
    }

    public StoredMessage getMessage(String messageId) {
        RMap<String, String> map = redisson.getMap(KEY_PREFIX + messageId);
        String topic = map.get("mqttTopic");
        if (topic == null) return null;
        return new StoredMessage(
                messageId,
                map.getOrDefault("clientId", ""),
                topic,
                map.getOrDefault("payload", ""),
                Status.valueOf(map.getOrDefault("status", "FAILED")),
                Integer.parseInt(map.getOrDefault("retryCount", "0")),
                Long.parseLong(map.getOrDefault("createdAt", "0"))
        );
    }

    public List<String> getInflightMessageIds(String clientId) {
        RList<String> list = redisson.getList(INFLIGHT_PREFIX + clientId);
        return new ArrayList<>(list);
    }

    public void removeFromInflight(String clientId, String messageId) {
        redisson.getList(INFLIGHT_PREFIX + clientId).remove(messageId);
    }

    public long getDeadMessageCount() {
        var keys = new java.util.ArrayList<String>(); for (String k : redisson.getKeys().getKeysByPattern(KEY_PREFIX + "*")) keys.add(k); return keys.stream()
                .filter(k -> Status.DEAD.name().equals(redisson.getMap(k).get("status")))
                .count();
    }


    public long getFailedMessageCount() {
        return redisson.getSet(FAILED_SET).size();
    }

    public void cleanDelivered() {
        var keys = redisson.getKeys().getKeysByPattern(KEY_PREFIX + "*");
        for (String k : keys) {
            if (Status.DELIVERED.name().equals(redisson.getMap(k).get("status"))) {
                redisson.getMap(k).delete();
            }
        }
    }

    public boolean isDuplicate(String idempotencyKey) {
        RBucket<String> bucket = redisson.getBucket(DEDUP_PREFIX + idempotencyKey);
        return !bucket.setIfAbsent("1", Duration.ofMinutes(30));
    }
}

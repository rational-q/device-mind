package com.devicemind.broker.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MQTT Session Redis 持久化存储
 * <p>
 * Redis 数据结构：
 * <ul>
 *   <li>{@code session:{clientId}} — Hash: connectedAt, lastHeartbeatAt, keepAlive, brokerNode</li>
 *   <li>{@code subs:{clientId}} — Set: topic filter 列表</li>
 *   <li>{@code sessions:active} — Set: 所有活跃 session 的 clientId</li>
 * </ul>
 * <p>
 * TTL 机制：session key 设置 TTL = keepAlive * 2 + 60s，
 * 每次心跳续期。如果设备长时间无心跳，Redis 自动过期清除。
 */
@Slf4j
@Component
public class SessionStore {

    private static final String SESSION_PREFIX = "session:";
    private static final String SUBS_PREFIX = "subs:";
    private static final String ACTIVE_SET_KEY = "sessions:active";
    private static final Duration MIN_TTL = Duration.ofMinutes(3);

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 保存会话到 Redis
     */
    public void save(DeviceSession session) {
        String key = SESSION_PREFIX + session.getClientId();
        redisTemplate.opsForHash().put(key, "clientId", session.getClientId());
        redisTemplate.opsForHash().put(key, "connectedAt", String.valueOf(session.getConnectedAt()));
        redisTemplate.opsForHash().put(key, "lastHeartbeatAt", String.valueOf(session.getLastHeartbeatAt()));
        redisTemplate.opsForHash().put(key, "keepAlive", String.valueOf(session.getKeepAlive()));

        long ttl = Math.max(session.getKeepAlive() * 2L + 60, MIN_TTL.getSeconds());
        redisTemplate.expire(key, Duration.ofSeconds(ttl));
        redisTemplate.opsForSet().add(ACTIVE_SET_KEY, session.getClientId());

        log.debug("会话已持久化到 Redis: clientId={}, ttl={}s", session.getClientId(), ttl);
    }

    /**
     * 续期 session TTL（心跳时调用）
     */
    public void renewHeartbeat(String clientId, int keepAlive) {
        String key = SESSION_PREFIX + clientId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForHash().put(key, "lastHeartbeatAt", String.valueOf(System.currentTimeMillis()));
            long ttl = Math.max(keepAlive * 2L + 60, MIN_TTL.getSeconds());
            redisTemplate.expire(key, Duration.ofSeconds(ttl));
        }
    }

    /**
     * 保存设备订阅到 Redis
     */
    public void saveSubscriptions(String clientId, Set<String> topics) {
        String key = SUBS_PREFIX + clientId;
        if (topics == null || topics.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        redisTemplate.delete(key);
        redisTemplate.opsForSet().add(key, topics.toArray(new String[0]));
        long ttl = Math.max(600, MIN_TTL.getSeconds());
        redisTemplate.expire(key, Duration.ofSeconds(ttl));
    }

    /**
     * 获取设备订阅列表
     */
    public Set<String> getSubscriptions(String clientId) {
        String key = SUBS_PREFIX + clientId;
        Set<String> topics = redisTemplate.opsForSet().members(key);
        return topics != null ? topics : Set.of();
    }

    /**
     * 从 Redis 移除会话
     */
    public void remove(String clientId) {
        redisTemplate.delete(SESSION_PREFIX + clientId);
        redisTemplate.delete(SUBS_PREFIX + clientId);
        redisTemplate.opsForSet().remove(ACTIVE_SET_KEY, clientId);
        log.debug("已从 Redis 清除会话: clientId={}", clientId);
    }

    /**
     * 检查会话是否在 Redis 中存在（用于判断 session recovery）
     */
    public boolean exists(String clientId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_PREFIX + clientId));
    }

    /**
     * 获取 Redis 中已持久化的 session 信息（不包含 channel 引用）
     */
    public PersistedSessionInfo getSessionInfo(String clientId) {
        String key = SESSION_PREFIX + clientId;
        String connectedAt = (String) redisTemplate.opsForHash().get(key, "connectedAt");
        if (connectedAt == null) return null;

        String lastHeartbeatAt = (String) redisTemplate.opsForHash().get(key, "lastHeartbeatAt");
        String keepAlive = (String) redisTemplate.opsForHash().get(key, "keepAlive");

        return new PersistedSessionInfo(
                clientId,
                Long.parseLong(connectedAt),
                lastHeartbeatAt != null ? Long.parseLong(lastHeartbeatAt) : 0,
                keepAlive != null ? Integer.parseInt(keepAlive) : 60
        );
    }

    /**
     * 获取所有持久化 session 的 clientId 列表（Broker 重启恢复用）
     */
    public List<String> getAllClientIds() {
        Set<String> members = redisTemplate.opsForSet().members(ACTIVE_SET_KEY);
        return members != null ? new ArrayList<>(members) : List.of();
    }

    /**
     * 持久化会话信息（不含 channel 引用）
     */
    public record PersistedSessionInfo(
            String clientId,
            long connectedAt,
            long lastHeartbeatAt,
            int keepAlive
    ) {}
}

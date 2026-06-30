package com.devicemind.broker.session;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class SessionStore {

    private static final String SESSION_PREFIX = "session:";
    private static final String SUBS_PREFIX = "subs:";
    private static final String ACTIVE_SET = "sessions:active";
    private static final Duration MIN_TTL = Duration.ofMinutes(3);

    @Autowired
    private RedissonClient redisson;

    public void save(DeviceSession session) {
        String key = SESSION_PREFIX + session.getClientId();
        RMap<String, String> map = redisson.getMap(key);
        map.put("clientId", session.getClientId());
        map.put("connectedAt", String.valueOf(session.getConnectedAt()));
        map.put("lastHeartbeatAt", String.valueOf(session.getLastHeartbeatAt()));
        map.put("keepAlive", String.valueOf(session.getKeepAlive()));

        long ttl = Math.max(session.getKeepAlive() * 2L + 60, MIN_TTL.getSeconds());
        map.expire(Duration.ofSeconds(ttl));
        redisson.getSet(ACTIVE_SET).add(session.getClientId());
    }

    public void renewHeartbeat(String clientId, int keepAlive) {
        RMap<String, String> map = redisson.getMap(SESSION_PREFIX + clientId);
        if (map.isExists()) {
            map.put("lastHeartbeatAt", String.valueOf(System.currentTimeMillis()));
            long ttl = Math.max(keepAlive * 2L + 60, MIN_TTL.getSeconds());
            map.expire(Duration.ofSeconds(ttl));
        }
    }

    public void saveSubscriptions(String clientId, Set<String> topics) {
        String key = SUBS_PREFIX + clientId;
        if (topics == null || topics.isEmpty()) {
            redisson.getSet(key).delete();
            return;
        }
        RSet<String> set = redisson.getSet(key);
        set.clear();
        set.addAll(topics);
        set.expire(Duration.ofSeconds(Math.max(600, MIN_TTL.getSeconds())));
    }

    public Set<String> getSubscriptions(String clientId) {
        Set<Object> raw = redisson.getSet(SUBS_PREFIX + clientId).readAll(); return raw.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
    }

    public void remove(String clientId) {
        redisson.getMap(SESSION_PREFIX + clientId).delete();
        redisson.getSet(SUBS_PREFIX + clientId).delete();
        redisson.getSet(ACTIVE_SET).remove(clientId);
    }

    public boolean exists(String clientId) {
        return redisson.getMap(SESSION_PREFIX + clientId).isExists();
    }

    public PersistedSessionInfo getSessionInfo(String clientId) {
        RMap<String, String> map = redisson.getMap(SESSION_PREFIX + clientId);
        if (!map.isExists()) return null;
        return new PersistedSessionInfo(
                clientId,
                Long.parseLong(map.getOrDefault("connectedAt", "0")),
                Long.parseLong(map.getOrDefault("lastHeartbeatAt", "0")),
                Integer.parseInt(map.getOrDefault("keepAlive", "60"))
        );
    }

    public List<String> getAllClientIds() {
        RSet<String> set = redisson.getSet(ACTIVE_SET);
        return new ArrayList<>(set.readAll());
    }

    public record PersistedSessionInfo(String clientId, long connectedAt, long lastHeartbeatAt, int keepAlive) {}
}

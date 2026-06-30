package com.devicemind.broker.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT 会话管理器
 * <p>
 * 双写模式：内存（ConcurrentHashMap）+ Redis（SessionStore）。
 * <ul>
 *   <li>内存：高效读写，支撑 Netty 事件循环的快速会话查找</li>
 *   <li>Redis：持久化，Broker 重启后恢复订阅和会话</li>
 * </ul>
 */
@Slf4j
@Component
public class SessionManager {

    @Autowired
    private SessionStore sessionStore;

    // clientId → 会话（内存主副本）
    private final ConcurrentHashMap<String, DeviceSession> sessions = new ConcurrentHashMap<>();
    // channel长Id → clientId
    private final ConcurrentHashMap<String, String> channelToClientId = new ConcurrentHashMap<>();

    /**
     * 注册设备连接（原子操作，内存 + Redis 双写）
     */
    public void register(DeviceSession session) {
        String clientId = session.getClientId();
        String channelKey = session.getChannel().id().asLongText();

        DeviceSession old = sessions.put(clientId, session);
        if (old != null) {
            log.warn("重复的设备ID {} 上线，踢掉旧连接", clientId);
            String oldKey = old.getChannel().id().asLongText();
            channelToClientId.remove(oldKey);
            old.getChannel().close();
        }
        channelToClientId.put(channelKey, clientId);

        // 持久化到 Redis
        try {
            sessionStore.save(session);
        } catch (Exception e) {
            log.error("会话持久化到 Redis 失败: clientId={}", clientId, e);
        }

        log.info("设备上线: clientId={}, 在线数: {}", clientId, sessions.size());
    }

    /**
     * 设备断开连接（内存 + Redis 双删）
     */
    public void unregister(Channel channel) {
        String longId = channel.id().asLongText();
        String clientId = channelToClientId.remove(longId);
        if (clientId != null) {
            sessions.remove(clientId);

            // 从 Redis 清除
            try {
                sessionStore.remove(clientId);
            } catch (Exception e) {
                log.error("Redis 会话清除失败: clientId={}", clientId, e);
            }

            log.info("设备下线: clientId={}, 在线数: {}", clientId, sessions.size());
        }
    }

    /**
     * 更新心跳时间（内存 + Redis 续期）
     */
    public void updateHeartbeat(Channel channel) {
        String clientId = channelToClientId.get(channel.id().asLongText());
        if (clientId != null) {
            DeviceSession session = sessions.get(clientId);
            if (session != null) {
                session.setLastHeartbeatAt(System.currentTimeMillis());

                // Redis TTL 续期
                try {
                    sessionStore.renewHeartbeat(clientId, session.getKeepAlive());
                } catch (Exception e) {
                    log.warn("Redis 心跳续期失败: clientId={}", clientId, e);
                }
            }
        }
    }

    /**
     * 根据 clientId 获取会话
     */
    public DeviceSession getSession(String clientId) {
        return sessions.get(clientId);
    }

    /**
     * 根据 Channel 获取 clientId
     */
    public String getClientId(Channel channel) {
        return channelToClientId.get(channel.id().asLongText());
    }

    /**
     * 获取所有在线会话（用于优雅关闭时清理 Redis）
     */
    public ConcurrentHashMap<String, DeviceSession> getAllSessions() {
        return sessions;
    }

    /**
     * 当前在线设备数
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 检查 Redis 中是否存在旧会话（用于 sessionPresent 判断）
     */
    public boolean hasPersistedSession(String clientId) {
        try {
            return sessionStore.exists(clientId);
        } catch (Exception e) {
            log.warn("Redis 会话检查失败: clientId={}", clientId, e);
            return false;
        }
    }

    /**
     * 从 Redis 恢复订阅（设备重连时调用）
     */
    public void restoreSubscriptions(String clientId) {
        // 此处由 SubscribeHandler 在设备重新订阅时调用 SessionStore.saveSubscriptions
        // 如果需要 Broker 侧主动恢复，需要通过 MQTT SUBSCRIBE 重新下发
        log.info("设备重连，尝试恢复会话: clientId={}", clientId);
    }
}

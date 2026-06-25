package com.devicemind.broker.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionManager {
    // clientId -> 会话
    private final ConcurrentHashMap<String, DeviceSession> sessions = new ConcurrentHashMap<>();
    // channel短Id -> clientId
    private final ConcurrentHashMap<String, String> channelToClientId = new ConcurrentHashMap<>();

    /**
     * 注册设备连接
     */
    public void register(DeviceSession session) {
        String clientId = session.getClientId();
        DeviceSession old = sessions.get(clientId);
        if (old != null) {
            log.warn("重复的设备ID {} 上线，踢掉旧连接", clientId);
            old.getChannel().close();
        }
        sessions.put(clientId, session);
        channelToClientId.put(session.getChannel().id().asShortText(), clientId);
        log.info("设备上线: clientId={}, 在线数: {}", clientId, sessions.size());
    }

    /**
     * 设备断开连接
     */
    public void unregister(Channel channel) {
        String shortId = channel.id().asShortText();
        String clientId = channelToClientId.remove(shortId);
        if (clientId != null) {
            sessions.remove(clientId);
            log.info("设备下线: clientId={}, 在线数: {}", clientId, sessions.size());
        }
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeat(Channel channel) {
        String clientId = channelToClientId.get(channel.id().asShortText());
        if (clientId != null) {
            DeviceSession session = sessions.get(clientId);
            if (session != null) {
                session.setLastHeartbeatAt(System.currentTimeMillis());
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
     * 当前在线设备数
     */
    public int getOnlineCount() {
        return sessions.size();
    }
}

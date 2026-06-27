package com.devicemind.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 处理器 — 向大屏推送实时设备数据
 * <p>
 * 连接端点: ws://host:8080/ws/monitor
 */
@Slf4j
@Component
public class DeviceDataWebSocketHandler extends TextWebSocketHandler {

    /** 所有在线 WebSocket 客户端 */
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    private final ObjectMapper objectMapper;

    public DeviceDataWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 客户端连接: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 客户端断开: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端消息暂不处理，仅用于推送
    }

    /**
     * 广播设备数据到所有 WebSocket 客户端
     */
    public void broadcastDeviceData(String deviceId, String attrName, Object value, long timestamp) {
        broadcast(Map.of(
                "type", "device_data",
                "deviceId", deviceId,
                "attrName", attrName,
                "value", value,
                "timestamp", timestamp
        ));
    }

    /**
     * 广播告警事件
     */
    public void broadcastAlert(String deviceId, String ruleName, String level, Object currentValue) {
        broadcast(Map.of(
                "type", "alert",
                "deviceId", deviceId,
                "ruleName", ruleName,
                "level", level,
                "currentValue", currentValue
        ));
    }

    /** 广播 JSON 消息 */
    private void broadcast(Map<String, Object> data) {
        if (sessions.isEmpty()) return;
        try {
            String json = objectMapper.writeValueAsString(data);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (Exception e) {
                        log.warn("WebSocket 发送失败: sessionId={}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket 广播序列化失败", e);
        }
    }

    /** 当前在线客户端数 */
    public int getOnlineCount() {
        return sessions.size();
    }
}

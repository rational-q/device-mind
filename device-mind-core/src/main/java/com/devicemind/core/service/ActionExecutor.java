package com.devicemind.core.service;

import com.devicemind.core.client.BrokerCommandClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 场景联动动作执行器
 * <p>
 * 按顺序执行动作链：COMMAND（指令下发）/ DELAY（延迟）/ SMS（短信）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionExecutor {

    private final BrokerCommandClient brokerCommandClient;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    /**
     * 执行动作链
     *
     * @param actionsJson 动作列表 JSON
     * @return 每个动作的执行结果（按顺序）
     */
    public List<Map<String, Object>> execute(String actionsJson) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            List<Map<String, Object>> actions = objectMapper.readValue(actionsJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            for (int i = 0; i < actions.size(); i++) {
                Map<String, Object> action = actions.get(i);
                String type = (String) action.get("type");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("index", i);
                result.put("type", type);

                try {
                    switch (type) {
                        case "COMMAND" -> executeCommand(action, result);
                        case "SMS" -> executeSms(action, result);
                        case "DELAY" -> executeDelay(action, result);
                        default -> {
                            result.put("success", false);
                            result.put("error", "未知动作类型: " + type);
                        }
                    }
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    log.warn("动作执行失败: index={}, type={}, err={}", i, type, e.getMessage());
                }

                results.add(result);
            }
        } catch (Exception e) {
            log.error("解析动作列表失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", "解析动作JSON失败: " + e.getMessage());
            results.add(error);
        }

        return results;
    }

    /** 执行指令下发 */
    private void executeCommand(Map<String, Object> action, Map<String, Object> result) {
        String targetDeviceId = (String) action.get("targetDeviceId");
        String command = (String) action.get("command");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = action.get("params") instanceof Map
                ? (Map<String, Object>) action.get("params") : new HashMap<>();

        String idempotencyKey = UUID.randomUUID().toString();
        boolean sent = brokerCommandClient.sendCommand(targetDeviceId, command, params, idempotencyKey);

        result.put("success", sent);
        result.put("targetDeviceId", targetDeviceId);
        result.put("command", command);
        result.put("message", sent ? "指令已发送" : "设备离线或发送失败");
    }

    /** 执行短信发送 */
    private void executeSms(Map<String, Object> action, Map<String, Object> result) {
        @SuppressWarnings("unchecked")
        List<String> phoneNumbers = action.get("phoneNumbers") instanceof List
                ? (List<String>) action.get("phoneNumbers") : List.of();
        String content = (String) action.getOrDefault("content", "");

        boolean sent = smsService.send(phoneNumbers, content);
        result.put("success", sent);
        result.put("phoneNumbers", phoneNumbers);
        result.put("content", content);
        result.put("message", sent ? "短信已发送" : "短信发送失败");
    }

    /** 执行延迟 */
    private void executeDelay(Map<String, Object> action, Map<String, Object> result) {
        int seconds = action.get("seconds") instanceof Number
                ? ((Number) action.get("seconds")).intValue() : 0;
        if (seconds > 0) {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result.put("success", false);
                result.put("error", "延迟被中断");
                return;
            }
        }
        result.put("success", true);
        result.put("seconds", seconds);
        result.put("message", "延迟 " + seconds + " 秒");
    }
}

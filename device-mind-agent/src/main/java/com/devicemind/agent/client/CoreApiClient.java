package com.devicemind.agent.client;

import com.devicemind.agent.config.CoreServiceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Core 服务 REST API 客户端
 * <p>
 * 调用 Core 模块接口获取设备信息、时序数据、告警等，供 Function Calling 使用。
 * 所有返回结果均为 JSON 字符串，直接作为 tool 调用的 response content。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreApiClient {

    private final RestTemplate restTemplate;
    private final CoreServiceConfig config;
    private final ObjectMapper objectMapper;

    // ==================== 设备信息 ====================

    /**
     * 查询设备基本信息
     * Core 的 detail 接口用 Long id 查，故通过 list 接口按 deviceId 过滤
     */
    public String getDeviceInfo(String deviceId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("deviceId", deviceId);
            body.put("pageNum", 1);
            body.put("pageSize", 1);
            JsonNode result = doPost(config.getUrl() + "/device-mind/devices/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray() && records.size() > 0) {
                return objectMapper.writeValueAsString(records.get(0));
            }
            return "{\"error\":\"未找到设备: " + deviceId + "\"}";
        } catch (Exception e) {
            log.warn("查询设备信息失败: deviceId={}", deviceId, e);
            return "{\"error\":\"查询设备信息失败: " + e.getMessage() + "\"}";
        }
    }

    // ==================== 时序数据 ====================

    /**
     * 查询设备最近 N 小时的时序数据
     */
    public String getDeviceData(String deviceId, String attrName, Integer hours) {
        try {
            long now = System.currentTimeMillis() / 1000;
            int h = hours != null && hours > 0 ? hours : 1;
            long start = now - h * 3600L;

            Map<String, Object> body = new HashMap<>();
            body.put("deviceId", deviceId);
            body.put("attrName", attrName);
            body.put("start", start);
            body.put("end", now);
            body.put("pageNum", 1);
            body.put("pageSize", 200);

            JsonNode result = doPost(config.getUrl() + "/device-mind/device-data/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                return objectMapper.writeValueAsString(records);
            }
            return "[]";
        } catch (Exception e) {
            log.warn("查询设备数据失败: deviceId={}", deviceId, e);
            return "{\"error\":\"查询设备数据失败: " + e.getMessage() + "\"}";
        }
    }

    // ==================== 设备影子 ====================

    /**
     * 查询设备影子状态
     */
    public String getDeviceShadow(String deviceId) {
        try {
            String url = config.getUrl() + "/device-mind/shadows?deviceId=" + deviceId;
            JsonNode result = doGet(url);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("查询设备影子失败: deviceId={}", deviceId, e);
            return "{\"error\":\"查询设备影子失败: " + e.getMessage() + "\"}";
        }
    }

    // ==================== 告警历史 ====================

    /**
     * 查询设备最近 N 小时的告警记录
     */
    public String getAlertHistory(String deviceId, Integer hours) {
        try {
            long now = System.currentTimeMillis();
            int h = hours != null && hours > 0 ? hours : 24;
            long start = now - h * 3600L * 1000L;

            Map<String, Object> body = new HashMap<>();
            body.put("deviceId", deviceId);
            body.put("startTime", start);
            body.put("endTime", now);
            body.put("pageNum", 1);
            body.put("pageSize", 20);

            JsonNode result = doPost(config.getUrl() + "/device-mind/alerts/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                return objectMapper.writeValueAsString(records);
            }
            return "[]";
        } catch (Exception e) {
            log.warn("查询告警历史失败: deviceId={}", deviceId, e);
            return "{\"error\":\"查询告警历史失败: " + e.getMessage() + "\"}";
        }
    }

    // ==================== 告警规则 ====================

    /**
     * 查询告警规则列表
     */
    public String getAlertRules(String deviceType) {
        try {
            Map<String, Object> body = new HashMap<>();
            if (deviceType != null && !deviceType.isBlank()) {
                body.put("deviceType", deviceType);
            }
            body.put("pageNum", 1);
            body.put("pageSize", 50);

            JsonNode result = doPost(config.getUrl() + "/device-mind/alert-rules/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                return objectMapper.writeValueAsString(records);
            }
            return "[]";
        } catch (Exception e) {
            log.warn("查询告警规则失败", e);
            return "{\"error\":\"查询告警规则失败: " + e.getMessage() + "\"}";
        }
    }

    // ==================== 内部 HTTP 辅助 ====================

    /** GET 请求，返回 Result.data 的 JsonNode */
    private JsonNode doGet(String url) {
        log.debug("CoreApi GET: {}", url);
        JsonNode resp = restTemplate.getForObject(url, JsonNode.class);
        return unwrapData(resp, url);
    }

    /** POST 请求，返回 Result.data 的 JsonNode */
    private JsonNode doPost(String url, Object body) {
        log.debug("CoreApi POST: {}, body={}", url, body);
        JsonNode resp = restTemplate.postForObject(url, body, JsonNode.class);
        return unwrapData(resp, url);
    }

    /** 从 Result<T> 中提取 data 字段 */
    private JsonNode unwrapData(JsonNode resp, String url) {
        if (resp == null) {
            throw new RuntimeException("Core 服务返回为空: " + url);
        }
        int code = resp.has("code") ? resp.get("code").asInt(500) : 500;
        if (code != 200) {
            String msg = resp.has("message") ? resp.get("message").asText() : "未知错误";
            throw new RuntimeException("Core 服务返回错误: code=" + code + ", msg=" + msg);
        }
        return resp.get("data");
    }
}

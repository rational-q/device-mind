package com.devicemind.agent.client;

import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.agent.config.CoreServiceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.devicemind.common.utils.JsonUtil;
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
public class CoreApiClient {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CoreServiceConfig config;

    /** 聚合统计时单页大小 */
    private static final int STAT_PAGE_SIZE = 200;
    /** 聚合统计时最大翻页数（防止设备/告警量过大时无限翻页） */
    private static final int MAX_STAT_PAGES = 50;

    /** 构造统一转义的错误 JSON，避免 message 含引号/换行导致非法 JSON */
    private static String errorJson(String message) {
        try {
            return JsonUtil.toJson(Map.of("error", message == null ? "" : message));
        } catch (Exception e) {
            return "{\"error\":\"内部错误\"}";
        }
    }
    // ==================== 设备信息 ====================

    /**
     * 查询设备基本信息
     * Core 的 detail 接口用 Long id 查，list 接口的 deviceId 是模糊匹配，
     * 故拉取候选后在内存中按 deviceId 精确匹配，避免 temp-1 命中 temp-10。
     */
    public String getDeviceInfo(String deviceId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("deviceId", deviceId);
            body.put("pageNum", 1);
            body.put("pageSize", 50);
            JsonNode result = doPost(config.getUrl() + "/device-mind/core/devices/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                for (JsonNode r : records) {
                    if (r.has("deviceId") && deviceId.equals(r.get("deviceId").asText())) {
                        return JsonUtil.toJson(r);
                    }
                }
            }
            return errorJson("未找到设备: " + deviceId);
        } catch (Exception e) {
            log.warn("查询设备信息失败: deviceId={}", deviceId, e);
            return errorJson("查询设备信息失败: " + e.getMessage());
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

            JsonNode result = doPost(config.getUrl() + "/device-mind/core/device-data/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                return JsonUtil.toJson(records);
            }
            return "[]";
        } catch (Exception e) {
            log.warn("查询设备数据失败: deviceId={}", deviceId, e);
            return errorJson("查询设备数据失败: " + e.getMessage());
        }
    }

    // ==================== 设备影子 ====================

    /**
     * 查询设备影子状态
     */
    public String getDeviceShadow(String deviceId) {
        try {
            String url = config.getUrl() + "/device-mind/core/shadows?deviceId=" + deviceId;
            JsonNode result = doGet(url);
            if (result == null || result.isNull()) {
                return errorJson("设备无影子数据: " + deviceId);
            }
            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.warn("查询设备影子失败: deviceId={}", deviceId, e);
            return errorJson("查询设备影子失败: " + e.getMessage());
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

            JsonNode result = doPost(config.getUrl() + "/device-mind/core/alerts/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                return JsonUtil.toJson(records);
            }
            return "[]";
        } catch (Exception e) {
            log.warn("查询告警历史失败: deviceId={}", deviceId, e);
            return errorJson("查询告警历史失败: " + e.getMessage());
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

            JsonNode result = doPost(config.getUrl() + "/device-mind/core/alert-rules/list", body);
            JsonNode records = result.get("records");
            if (records != null && records.isArray()) {
                return JsonUtil.toJson(records);
            }
            return "[]";
        } catch (Exception e) {
            log.warn("查询告警规则失败", e);
            return errorJson("查询告警规则失败: " + e.getMessage());
        }
    }

    // ==================== 设备在线统计 ====================

    /**
     * 查询设备在线状态统计—全部设备或按 productKey 过滤
     */
    public String getDeviceStatusSummary(String deviceId, String productKey) {
        try {
            Map<String, Object> body = new HashMap<>();
            if (deviceId != null && !deviceId.isBlank()) body.put("deviceId", deviceId);
            if (productKey != null && !productKey.isBlank()) body.put("productKey", productKey);

            int total = 0, online = 0;
            for (int pageNum = 1; pageNum <= MAX_STAT_PAGES; pageNum++) {
                body.put("pageNum", pageNum);
                body.put("pageSize", STAT_PAGE_SIZE);
                JsonNode result = doPost(config.getUrl() + "/device-mind/core/devices/list", body);
                JsonNode records = result.get("records");
                if (records == null || !records.isArray() || records.isEmpty()) break;
                for (JsonNode r : records) {
                    total++;
                    String status = r.has("status") ? r.get("status").asText() : "";
                    if ("ONLINE".equalsIgnoreCase(status)) online++;
                }
                if (records.size() < STAT_PAGE_SIZE) break;
            }
            return String.format("{\"total\":%d,\"online\":%d,\"offline\":%d}", total, online, total - online);
        } catch (Exception e) {
            log.warn("查询设备状态统计失败", e);
            return errorJson("查询失败: " + e.getMessage());
        }
    }

    // ==================== 告警概览 ====================

    /**
     * 查询近 N 小时告警概览（按等级统计）
     */
    public String getAlertSummary(Integer hours) {
        try {
            long now = System.currentTimeMillis();
            int h = hours != null && hours > 0 ? hours : 1;
            long start = now - h * 3600L * 1000L;

            Map<String, Object> body = new HashMap<>();
            body.put("startTime", start);
            body.put("endTime", now);

            int critical = 0, warn = 0, info = 0;
            for (int pageNum = 1; pageNum <= MAX_STAT_PAGES; pageNum++) {
                body.put("pageNum", pageNum);
                body.put("pageSize", STAT_PAGE_SIZE);
                JsonNode result = doPost(config.getUrl() + "/device-mind/core/alerts/list", body);
                JsonNode records = result.get("records");
                if (records == null || !records.isArray() || records.isEmpty()) break;
                for (JsonNode r : records) {
                    String level = r.has("level") ? r.get("level").asText().toUpperCase() : "";
                    switch (level) {
                        case "CRITICAL" -> critical++;
                        case "WARN", "WARNING" -> warn++;
                        default -> info++;
                    }
                }
                if (records.size() < STAT_PAGE_SIZE) break;
            }
            return String.format("{\"total\":%d,\"critical\":%d,\"warn\":%d,\"info\":%d,\"hours\":%d}",
                    critical + warn + info, critical, warn, info, h);
        } catch (Exception e) {
            log.warn("查询告警概览失败", e);
            return errorJson("查询失败: " + e.getMessage());
        }
    }

    // ==================== 指令执行统计 ====================

    /**
     * 查询指令执行统计—成功率
     */
    public String getCommandStats(String deviceId, Integer hours) {
        try {
            long now = System.currentTimeMillis();
            int h = hours != null && hours > 0 ? hours : 24;
            long start = now - h * 3600L * 1000L;

            Map<String, Object> body = new HashMap<>();
            body.put("deviceId", deviceId);
            body.put("startTime", start);
            body.put("endTime", now);

            int total = 0, success = 0, failed = 0, pending = 0;
            for (int pageNum = 1; pageNum <= MAX_STAT_PAGES; pageNum++) {
                body.put("pageNum", pageNum);
                body.put("pageSize", STAT_PAGE_SIZE);
                JsonNode result = doPost(config.getUrl() + "/device-mind/core/command-logs/list", body);
                JsonNode records = result.get("records");
                if (records == null || !records.isArray() || records.isEmpty()) break;
                for (JsonNode r : records) {
                    total++;
                    String status = r.has("status") ? r.get("status").asText().toUpperCase() : "";
                    switch (status) {
                        case "SUCCESS" -> success++;
                        case "EXPIRED", "FAILED" -> failed++;
                        default -> pending++;
                    }
                }
                if (records.size() < STAT_PAGE_SIZE) break;
            }
            double rate = total > 0 ? Math.round(success * 10000.0 / total) / 100.0 : 0;
            return String.format(
                    "{\"total\":%d,\"success\":%d,\"failed\":%d,\"pending\":%d,\"successRate\":%.1f,\"hours\":%d}",
                    total, success, failed, pending, rate, h);
        } catch (Exception e) {
            log.warn("查询指令统计失败", e);
            return errorJson("查询失败: " + e.getMessage());
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

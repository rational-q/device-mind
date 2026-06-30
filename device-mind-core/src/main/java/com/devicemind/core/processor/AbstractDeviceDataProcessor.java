package com.devicemind.core.processor;

import com.devicemind.common.kafka.model.DeviceDataPoint;
import com.devicemind.core.client.AlertAnalysisClient;
import com.devicemind.core.support.DeviceDataWebSocketHandler;
import com.devicemind.core.support.SmsSupport;
import com.devicemind.core.support.ActionSupport;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.model.entity.DmAlertRule;
import com.devicemind.core.model.entity.DmDeviceData;
import com.devicemind.core.model.entity.DmDeviceShadow;
import com.devicemind.core.model.entity.DmScene;
import com.devicemind.core.model.entity.DmSceneLog;
import com.devicemind.core.stdsvc.intf.IDmAlertRuleService;
import com.devicemind.core.stdsvc.intf.IDmAlertService;
import com.devicemind.core.stdsvc.intf.IDmDeviceDataService;
import com.devicemind.core.stdsvc.intf.IDmDeviceShadowService;
import com.devicemind.core.stdsvc.intf.IDmSceneLogService;
import com.devicemind.core.stdsvc.intf.IDmSceneService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.devicemind.common.utils.JsonUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.devicemind.common.utils.SnowflakeId;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 设备数据处理器抽象基类（模板方法模式）
 * <p>
 * 定义标准处理管道：save → shadow → alert → scene，所有实现内聚在此类中。
 * 子类只需声明 productKey，可重写钩子实现产品差异化。
 */
@Slf4j
public abstract class AbstractDeviceDataProcessor implements DeviceDataProcessor {

    // ---- 数据存储 ----
    @Autowired
    private IDmDeviceDataService dmDeviceDataService;
    @Autowired
    private IDmDeviceShadowService dmDeviceShadowService;
    // ---- 告警引擎 ----
    @Autowired
    private IDmAlertRuleService alertRuleService;
    @Autowired
    private IDmAlertService alertService;
    @Autowired
    private AlertAnalysisClient alertAnalysisClient;
    @Autowired
    private DeviceDataWebSocketHandler webSocketHandler;
    @Autowired
    private SmsSupport smsService;
    @Autowired
    private CacheManager cacheManager;

    // ---- 场景联动 ----
    @Autowired
    private IDmSceneService sceneService;
    @Autowired
    private IDmSceneLogService sceneLogService;
    @Autowired
    private ActionSupport actionExecutor;

    @Value("${alert.critical.alert-phone:}")
    private String criticalAlertPhone;

    private final ExecutorService alertAnalysisExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "alert-analysis");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdown() {
        alertAnalysisExecutor.shutdown();
        try {
            if (!alertAnalysisExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                alertAnalysisExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            alertAnalysisExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ======================== 模板方法 ========================

    @Override
    public void process(List<DeviceDataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) return;

        preProcess(dataPoints);
        saveData(dataPoints);
        updateShadow(dataPoints);
        evaluateAlerts(dataPoints);
        evaluateScenes(dataPoints);
        postProcess(dataPoints);
    }

    // ---- 钩子 ----
    protected void preProcess(List<DeviceDataPoint> dataPoints) {}
    protected void postProcess(List<DeviceDataPoint> dataPoints) {}

    // ======================== 数据存储 ========================

    private void saveData(List<DeviceDataPoint> points) {
        dmDeviceDataService.saveData(points);
    }

    private void updateShadow(List<DeviceDataPoint> points) {
        Map<String, List<DeviceDataPoint>> grouped = points.stream()
                .collect(Collectors.groupingBy(DeviceDataPoint::getDeviceId));

        for (Map.Entry<String, List<DeviceDataPoint>> entry : grouped.entrySet()) {
            String deviceId = entry.getKey();
            Map<String, Object> attrs = new LinkedHashMap<>();
            entry.getValue().forEach(p -> attrs.put(p.getAttrName(), p.getValue()));

            String reportedJson;
            try {
                reportedJson = JsonUtil.toJson(attrs);
            } catch (JsonUtil.JsonException e) {
                log.error("设备影子序列化失败: deviceId={}", deviceId, e);
                continue;
            }

            DmDeviceShadow shadow = new DmDeviceShadow()
                    .setDeviceId(deviceId)
                    .setReported(reportedJson)
                    .setUpdatedDate(new Date());
            boolean updated = dmDeviceShadowService.lambdaUpdate()
                    .eq(DmDeviceShadow::getDeviceId, deviceId)
                    .setSql("REPORTED_VERSION = COALESCE(REPORTED_VERSION, 0) + 1")
                    .update(shadow);
            if (!updated) {
                shadow.setReportedVersion(1);
                dmDeviceShadowService.save(shadow);
            }
        }
    }

    // ======================== 告警评估 ========================

    private void evaluateAlerts(List<DeviceDataPoint> points) {
        String productKey = supportedProductKey();
        List<DmAlertRule> rules = cacheManager.getCache("alertRules")
                .get(productKey + ":enabled", () ->
                        alertRuleService.lambdaQuery()
                                .eq(DmAlertRule::getDeviceType, productKey)
                                .eq(DmAlertRule::getEnabled, true)
                                .list());
        if (rules == null || rules.isEmpty()) return;

        for (DeviceDataPoint point : points) {
            for (DmAlertRule rule : rules) {
                if (!rule.getAttrName().equals(point.getAttrName())) continue;
                if (!evaluateCondition(point, rule)) continue;
                if (!isWindowSustained(point, rule)) continue;
                if (hasActiveAlert(point.getDeviceId(), rule.getId())) continue;
                createAlert(point, rule);
            }
        }
    }

    private boolean evaluateCondition(DeviceDataPoint point, DmAlertRule rule) {
        double value = toDouble(point.getValue());
        double threshold = rule.getThreshold();
        return switch (rule.getOperator()) {
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            case "==" -> value == threshold;
            default -> false;
        };
    }

    private boolean isWindowSustained(DeviceDataPoint point, DmAlertRule rule) {
        long windowStart = point.getTimestamp() - rule.getDurationSeconds();
        try {
            List<DmDeviceData> windowData = dmDeviceDataService.lambdaQuery()
                    .eq(DmDeviceData::getDeviceId, point.getDeviceId())
                    .eq(DmDeviceData::getAttrName, rule.getAttrName())
                    .ge(DmDeviceData::getTime, Instant.ofEpochSecond(windowStart))
                    .le(DmDeviceData::getTime, Instant.ofEpochSecond(point.getTimestamp()))
                    .orderByDesc(DmDeviceData::getTime)
                    .list();
            if (windowData.isEmpty()) return true;
            for (DmDeviceData data : windowData) {
                if (data.getValue() != null && !evaluateConditionRaw(data.getValue(), rule))
                    return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("滑动窗口查询异常，降级为允许触发: deviceId={}, attr={}", point.getDeviceId(), rule.getAttrName());
            return true;
        }
    }

    private boolean evaluateConditionRaw(double value, DmAlertRule rule) {
        double threshold = rule.getThreshold();
        return switch (rule.getOperator()) {
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            case "==" -> value == threshold;
            default -> false;
        };
    }

    private boolean hasActiveAlert(String deviceId, Long ruleId) {
        return alertService.lambdaQuery()
                .eq(DmAlert::getDeviceId, deviceId)
                .eq(DmAlert::getRuleId, ruleId)
                .eq(DmAlert::getStatus, "TRIGGERED")
                .count() > 0;
    }

    private void createAlert(DeviceDataPoint point, DmAlertRule rule) {
        DmAlert alert = new DmAlert();
        alert.setDeviceId(point.getDeviceId());
        alert.setRuleId(rule.getId());
        alert.setRuleName(rule.getRuleName());
        alert.setLevel(rule.getLevel());
        alert.setMetric(rule.getAttrName());
        alert.setCurrentValue(toDouble(point.getValue()));
        alert.setThreshold(rule.getThreshold());
        alert.setStatus("TRIGGERED");
        alert.setTriggeredAt(new Date());
        alertService.save(alert);
        log.info("触发告警: deviceId={}, rule={}, value={}, threshold={}, level={}",
                point.getDeviceId(), rule.getRuleName(), point.getValue(), rule.getThreshold(), rule.getLevel());

        webSocketHandler.broadcastAlert(alert.getDeviceId(), alert.getRuleName(),
                alert.getLevel(), alert.getCurrentValue());

        if ("CRITICAL".equals(rule.getLevel())
                && criticalAlertPhone != null && !criticalAlertPhone.isBlank()) {
            smsService.send(List.of(criticalAlertPhone),
                    "【DeviceMind】严重告警: " + alert.getRuleName()
                            + " - 设备 " + alert.getDeviceId()
                            + " 当前值: " + alert.getCurrentValue());
        }

        Long alertId = alert.getId();
        CompletableFuture.runAsync(() -> {
            try {
                DmAlert fresh = alertService.getById(alertId);
                if (fresh == null) return;
                String analysis = alertAnalysisClient.analyze(fresh);
                if (analysis != null) {
                    fresh.setAiAnalysis(analysis);
                    alertService.updateById(fresh);
                }
            } catch (Exception e) {
                log.warn("AI 分析回写异常: alertId={}", alertId, e);
            }
        }, alertAnalysisExecutor);
    }

    // ======================== 场景联动 ========================

    private void evaluateScenes(List<DeviceDataPoint> points) {
        List<DmScene> scenes = cacheManager.getCache("scenes")
                .get("enabled", () ->
                        sceneService.lambdaQuery()
                                .eq(DmScene::getEnabled, true)
                                .list());
        if (scenes == null || scenes.isEmpty()) return;

        for (DeviceDataPoint point : points) {
            for (DmScene scene : scenes) {
                try {
                    if (!matchSceneCondition(point, scene)) continue;
                    executeSceneActions(point, scene);
                } catch (Exception e) {
                    log.warn("场景评估异常: sceneId={}, deviceId={}", scene.getId(), point.getDeviceId(), e);
                }
            }
        }
    }

    private boolean matchSceneCondition(DeviceDataPoint point, DmScene scene) throws Exception {
        List<Map<String, Object>> conditions = JsonUtil.fromJson(
                scene.getConditions(), new TypeReference<List<Map<String, Object>>>() {});
        for (Map<String, Object> cond : conditions) {
            String attr = (String) cond.get("attr");
            if (!attr.equals(point.getAttrName())) continue;
            String operator = (String) cond.get("operator");
            double threshold = ((Number) cond.get("value")).doubleValue();
            double value = toDouble(point.getValue());
            boolean matched = switch (operator) {
                case ">" -> value > threshold;
                case ">=" -> value >= threshold;
                case "<" -> value < threshold;
                case "<=" -> value <= threshold;
                case "==" -> value == threshold;
                default -> false;
            };
            if (matched) return true;
        }
        return false;
    }

    private void executeSceneActions(DeviceDataPoint point, DmScene scene) {
        log.info("场景触发: sceneId={}, sceneName={}, deviceId={}", scene.getId(), scene.getName(), point.getDeviceId());
        List<Map<String, Object>> actionResults = actionExecutor.execute(scene.getActions());

        String actionsResultJson;
        String status = "SUCCESS";
        try {
            actionsResultJson = JsonUtil.toJson(actionResults);
            boolean allSuccess = actionResults.stream().allMatch(r -> Boolean.TRUE.equals(r.get("success")));
            boolean anySuccess = actionResults.stream().anyMatch(r -> Boolean.TRUE.equals(r.get("success")));
            if (!allSuccess && anySuccess) status = "PARTIAL";
            else if (!anySuccess) status = "FAILED";
        } catch (Exception e) {
            actionsResultJson = "[]";
            status = "FAILED";
        }

        DmSceneLog logEntry = new DmSceneLog();
        logEntry.setId(snowflakeId());
        logEntry.setSceneId(scene.getId());
        logEntry.setSceneName(scene.getName());
        logEntry.setDeviceId(point.getDeviceId());
        logEntry.setTriggeredAt(new Date());
        logEntry.setActionsResult(actionsResultJson);
        logEntry.setStatus(status);
        logEntry.setCreatedDate(new Date());
        sceneLogService.save(logEntry);
        log.info("场景执行完成: sceneId={}, status={}", scene.getId(), status);
    }

    private long snowflakeId() {
        return SnowflakeId.nextId();
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }
}

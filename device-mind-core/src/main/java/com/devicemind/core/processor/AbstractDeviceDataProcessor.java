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

    /**
     * 场景动作执行线程池。
     * <p>
     * 场景动作链里可能含 DELAY（Thread.sleep），若在 Kafka 消费线程内同步执行会阻塞
     * 整批消息 ack、拖慢入库。故将场景动作执行异步化，与数据入库解耦。
     */
    private final ExecutorService sceneActionExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "scene-action");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdown() {
        shutdownExecutor(alertAnalysisExecutor);
        shutdownExecutor(sceneActionExecutor);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
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
                boolean conditionMet = evaluateCondition(point, rule);
                if (conditionMet) {
                    if (!isWindowSustained(point, rule)) continue;
                    if (hasActiveAlert(point.getDeviceId(), rule.getId())) continue;
                    createAlert(point, rule);
                } else {
                    // 指标已恢复正常 → 自动将活跃告警置为 RESOLVED
                    autoResolveAlerts(point.getDeviceId(), rule.getId());
                }
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

    /**
     * 判定"持续时间窗口"内是否真正满足告警条件。
     * <p>
     * 规则（duration &gt; 0 时）：窗口 [t-duration, t] 内
     * <ol>
     *   <li>必须有历史数据点（空窗口不触发，避免单点误判）；</li>
     *   <li>窗口内所有点都满足条件（任一点不满足即视为未持续）；</li>
     *   <li>最早的数据点时间必须已到达/早于 windowStart，
     *       即窗口被数据真正覆盖满 N 秒，否则说明刚开始超阈值、还不够时长。</li>
     * </ol>
     * duration &lt;= 0 时表示无持续要求，当前点满足即触发。
     */
    private boolean isWindowSustained(DeviceDataPoint point, DmAlertRule rule) {
        int duration = rule.getDurationSeconds() != null ? rule.getDurationSeconds() : 0;
        if (duration <= 0) {
            // 无持续要求：当前点已在 evaluateCondition 判过，直接放行
            return true;
        }
        long windowStart = point.getTimestamp() - duration;
        try {
            List<DmDeviceData> windowData = dmDeviceDataService.lambdaQuery()
                    .eq(DmDeviceData::getDeviceId, point.getDeviceId())
                    .eq(DmDeviceData::getAttrName, rule.getAttrName())
                    .ge(DmDeviceData::getTime, Instant.ofEpochSecond(windowStart))
                    .le(DmDeviceData::getTime, Instant.ofEpochSecond(point.getTimestamp()))
                    .orderByAsc(DmDeviceData::getTime)
                    .list();

            // 1. 窗口内无数据 → 不足以判定持续，不触发
            if (windowData.isEmpty()) return false;

            // 2. 窗口内所有点都必须满足条件
            for (DmDeviceData data : windowData) {
                if (data.getValue() == null || !evaluateConditionRaw(data.getValue(), rule)) {
                    return false;
                }
            }

            // 3. 最早的数据点必须已覆盖 windowStart（数据真正跨越了整个窗口）
            DmDeviceData earliest = windowData.get(0);
            if (earliest.getTime() == null
                    || earliest.getTime().getEpochSecond() > windowStart) {
                // 窗口起点没有数据覆盖，说明超阈值时长还不够 duration 秒
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("滑动窗口查询异常，为避免漏报降级为允许触发: deviceId={}, attr={}",
                    point.getDeviceId(), rule.getAttrName(), e);
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

    /**
     * 是否存在活跃告警（TRIGGERED 或 CONFIRMED 均视为活跃，避免确认后重复触发）
     */
    private boolean hasActiveAlert(String deviceId, Long ruleId) {
        return alertService.lambdaQuery()
                .eq(DmAlert::getDeviceId, deviceId)
                .eq(DmAlert::getRuleId, ruleId)
                .in(DmAlert::getStatus, "TRIGGERED", "CONFIRMED")
                .count() > 0;
    }

    /**
     * 指标恢复正常时，将该设备+规则下所有活跃告警（TRIGGERED/CONFIRMED）
     * 自动置为 RESOLVED 并记录恢复时间。
     */
    private void autoResolveAlerts(String deviceId, Long ruleId) {
        try {
            DmAlert update = new DmAlert();
            update.setStatus("RESOLVED");
            update.setResolvedAt(new Date());
            boolean resolved = alertService.lambdaUpdate()
                    .eq(DmAlert::getDeviceId, deviceId)
                    .eq(DmAlert::getRuleId, ruleId)
                    .in(DmAlert::getStatus, "TRIGGERED", "CONFIRMED")
                    .update(update);
            if (resolved) {
                log.info("指标恢复正常，自动恢复告警: deviceId={}, ruleId={}", deviceId, ruleId);
            }
        } catch (Exception e) {
            log.warn("自动恢复告警失败: deviceId={}, ruleId={}", deviceId, ruleId, e);
        }
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
                    // 场景动作可能含 DELAY，异步执行避免阻塞 Kafka 消费线程
                    CompletableFuture.runAsync(
                            () -> executeSceneActions(point, scene), sceneActionExecutor);
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
            if (!matched) continue;

            // 持续时间校验：条件配了 duration(秒) 时，需窗口内持续满足才算命中
            Object durationObj = cond.get("duration");
            if (durationObj instanceof Number n && n.intValue() > 0) {
                if (!isSceneWindowSustained(point, attr, operator, threshold, n.intValue())) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 场景条件的持续时间窗口判定（复用与告警一致的语义）
     */
    private boolean isSceneWindowSustained(DeviceDataPoint point, String attr,
                                           String operator, double threshold, int duration) {
        long windowStart = point.getTimestamp() - duration;
        try {
            List<DmDeviceData> windowData = dmDeviceDataService.lambdaQuery()
                    .eq(DmDeviceData::getDeviceId, point.getDeviceId())
                    .eq(DmDeviceData::getAttrName, attr)
                    .ge(DmDeviceData::getTime, Instant.ofEpochSecond(windowStart))
                    .le(DmDeviceData::getTime, Instant.ofEpochSecond(point.getTimestamp()))
                    .orderByAsc(DmDeviceData::getTime)
                    .list();
            if (windowData.isEmpty()) return false;
            for (DmDeviceData data : windowData) {
                if (data.getValue() == null || !compare(data.getValue(), operator, threshold)) {
                    return false;
                }
            }
            DmDeviceData earliest = windowData.get(0);
            return earliest.getTime() != null && earliest.getTime().getEpochSecond() <= windowStart;
        } catch (Exception e) {
            log.warn("场景滑动窗口查询异常，降级为允许触发: deviceId={}, attr={}", point.getDeviceId(), attr, e);
            return true;
        }
    }

    private boolean compare(double value, String operator, double threshold) {
        return switch (operator) {
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            case "==" -> value == threshold;
            default -> false;
        };
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

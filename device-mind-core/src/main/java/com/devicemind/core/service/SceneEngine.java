package com.devicemind.core.service;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.model.entity.DmScene;
import com.devicemind.core.model.entity.DmSceneLog;
import com.devicemind.core.stdsvc.intf.IDmSceneLogService;
import com.devicemind.core.stdsvc.intf.IDmSceneService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 场景联动引擎 — 设备数据入库后评估场景条件，匹配则执行动作链
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneEngine {

    private final IDmSceneService sceneService;
    private final IDmSceneLogService sceneLogService;
    private final ActionExecutor actionExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 评估一批数据点是否匹配场景条件
     *
     * @param points     设备数据点
     * @param productKey 产品类型（暂未使用，保留参数兼容调用方）
     */
    public void evaluate(List<DeviceDataPoint> points, String productKey) {
        if (points == null || points.isEmpty()) return;

        // 加载所有启用的场景（场景条件通过 attr 名称自动匹配）
        List<DmScene> scenes = sceneService.lambdaQuery()
                .eq(com.devicemind.core.model.entity.DmScene::getEnabled, true)
                .list();

        if (scenes.isEmpty()) return;

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

    /**
     * 检查数据点是否满足场景触发条件
     * <p>
     * conditions JSON: [{"attr":"temperature","operator":">","value":40,"duration":30}]
     */
    private boolean matchSceneCondition(DeviceDataPoint point, DmScene scene) throws Exception {
        List<Map<String, Object>> conditions = objectMapper.readValue(
                scene.getConditions(),
                new TypeReference<List<Map<String, Object>>>() {});

        for (Map<String, Object> cond : conditions) {
            String attr = (String) cond.get("attr");
            if (!attr.equals(point.getAttrName())) continue;

            String operator = (String) cond.get("operator");
            double threshold = ((Number) cond.get("value")).doubleValue();
            double value = toDouble(point.getValue());

            boolean matched = switch (operator) {
                case ">"  -> value > threshold;
                case ">=" -> value >= threshold;
                case "<"  -> value < threshold;
                case "<=" -> value <= threshold;
                case "==" -> value == threshold;
                default   -> false;
            };

            if (matched) {
                log.debug("场景条件匹配: sceneId={}, deviceId={}, attr={}, value={}, op={}, threshold={}",
                        scene.getId(), point.getDeviceId(), attr, value, operator, threshold);
                return true;
            }
        }
        return false;
    }

    /** 执行场景动作并记录日志 */
    private void executeSceneActions(DeviceDataPoint point, DmScene scene) {
        log.info("场景触发: sceneId={}, sceneName={}, deviceId={}",
                scene.getId(), scene.getName(), point.getDeviceId());

        // 执行动作链
        List<Map<String, Object>> actionResults = actionExecutor.execute(scene.getActions());

        // 汇总状态
        String actionsResultJson;
        String status = "SUCCESS";
        try {
            actionsResultJson = objectMapper.writeValueAsString(actionResults);
            boolean allSuccess = actionResults.stream()
                    .allMatch(r -> Boolean.TRUE.equals(r.get("success")));
            boolean anySuccess = actionResults.stream()
                    .anyMatch(r -> Boolean.TRUE.equals(r.get("success")));
            if (!allSuccess && anySuccess) status = "PARTIAL";
            else if (!anySuccess) status = "FAILED";
        } catch (Exception e) {
            actionsResultJson = "[]";
            status = "FAILED";
        }

        // 记录执行日志
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

        log.info("场景执行完成: sceneId={}, status={}, actions={}",
                scene.getId(), status, actionResults.size());
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    /** 简易雪花ID生成（生产环境替换为统一ID生成器） */
    private long snowflakeId() {
        return System.currentTimeMillis() << 12 | (long) (Math.random() * 4096);
    }
}

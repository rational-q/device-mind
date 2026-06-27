package com.devicemind.core.service;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.client.AlertAnalysisClient;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.model.entity.DmAlertRule;
import com.devicemind.core.model.entity.DmDeviceData;
import com.devicemind.core.persistence.mapper.timescale.DmDeviceDataMapper;
import com.devicemind.core.stdsvc.intf.IDmAlertRuleService;
import com.devicemind.core.stdsvc.intf.IDmAlertService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 告警引擎 — 在设备数据入库后评估告警规则
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngine {

    private final IDmAlertRuleService alertRuleService;
    private final IDmAlertService alertService;
    private final DmDeviceDataMapper deviceDataMapper;
    private final AlertAnalysisClient alertAnalysisClient;
    private final DeviceDataWebSocketHandler webSocketHandler;
    private final SmsService smsService;

    @Value("${alert.critical.alert-phone:}")
    private String criticalAlertPhone;

    private final ExecutorService alertAnalysisExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "alert-analysis");
        t.setDaemon(true);
        return t;
    });

    /**
     * 对一批数据点评估对应产品下的所有启用的告警规则
     */
    public void evaluate(List<DeviceDataPoint> points, String productKey) {
        if (points == null || points.isEmpty()) return;

        List<DmAlertRule> rules = alertRuleService.lambdaQuery()
                .eq(DmAlertRule::getDeviceType, productKey)
                .eq(DmAlertRule::getEnabled, true)
                .list();

        if (rules.isEmpty()) return;

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
        return evaluateCondition(toDouble(point.getValue()), rule);
    }

    private boolean evaluateCondition(double value, DmAlertRule rule) {
        double threshold = rule.getThreshold();
        return switch (rule.getOperator()) {
            case ">"  -> value > threshold;
            case ">=" -> value >= threshold;
            case "<"  -> value < threshold;
            case "<=" -> value <= threshold;
            case "==" -> value == threshold;
            default   -> false;
        };
    }

    /**
     * 滑动窗口防抖 — 查询窗口期内是否所有数据点都持续违反阈值
     * <p>
     * 窗口 [当前时间 - durationSeconds, 当前时间] 内：
     * - 所有数据点都违反阈值 → 持续满足，触发告警
     * - 存在未违反的数据点 → 不持续，不触发（防抖）
     * - 窗口内无数据 → 首次上报，当前值已超阈值，允许触发
     */
    private boolean isWindowSustained(DeviceDataPoint point, DmAlertRule rule) {
        long windowStart = point.getTimestamp() - rule.getDurationSeconds();

        try {
            List<DmDeviceData> windowData = deviceDataMapper.selectList(
                    new LambdaQueryWrapper<DmDeviceData>()
                            .eq(DmDeviceData::getDeviceId, point.getDeviceId())
                            .eq(DmDeviceData::getAttrName, rule.getAttrName())
                            .ge(DmDeviceData::getTime, Instant.ofEpochSecond(windowStart))
                            .le(DmDeviceData::getTime, Instant.ofEpochSecond(point.getTimestamp()))
                            .orderByDesc(DmDeviceData::getTime)
            );

            // 窗口内无数据 → 首次上报，允许触发
            if (windowData.isEmpty()) {
                log.debug("滑动窗口无历史数据, deviceId={}, attr={}, 允许触发",
                        point.getDeviceId(), rule.getAttrName());
                return true;
            }

            // 检查是否所有窗口内数据点都违反阈值
            for (DmDeviceData data : windowData) {
                if (data.getValue() != null && !evaluateCondition(data.getValue(), rule)) {
                    log.debug("滑动窗口内存在不违反阈值的数据, deviceId={}, attr={}, value={}",
                            point.getDeviceId(), rule.getAttrName(), data.getValue());
                    return false;
                }
            }

            log.debug("滑动窗口持续满足条件, deviceId={}, attr={}, 点数={}",
                    point.getDeviceId(), rule.getAttrName(), windowData.size());
            return true;

        } catch (Exception e) {
            log.warn("滑动窗口查询异常，降级为允许触发: deviceId={}, attr={}, err={}",
                    point.getDeviceId(), rule.getAttrName(), e.getMessage());
            return true;
        }
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

        // WebSocket 广播告警事件
        webSocketHandler.broadcastAlert(alert.getDeviceId(), alert.getRuleName(),
                alert.getLevel(), alert.getCurrentValue());

        // CRITICAL 告警自动发短信
        if ("CRITICAL".equals(rule.getLevel())
                && criticalAlertPhone != null && !criticalAlertPhone.isBlank()) {
            smsService.send(List.of(criticalAlertPhone),
                    "【DeviceMind】严重告警: " + alert.getRuleName()
                            + " - 设备 " + alert.getDeviceId()
                            + " 当前值: " + alert.getCurrentValue());
        }

        // 异步调用 Agent AI 分析
        Long alertId = alert.getId();
        CompletableFuture.runAsync(() -> {
            try {
                DmAlert fresh = alertService.getById(alertId);
                if (fresh == null) return;
                String analysis = alertAnalysisClient.analyze(fresh);
                if (analysis != null) {
                    fresh.setAiAnalysis(analysis);
                    alertService.updateById(fresh);
                    log.info("AI 分析结果已回写: alertId={}", alertId);
                }
            } catch (Exception e) {
                log.warn("AI 分析回写异常: alertId={}", alertId, e);
            }
        }, alertAnalysisExecutor);
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

}

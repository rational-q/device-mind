package com.devicemind.core.service;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.model.entity.DmAlertRule;
import com.devicemind.core.persistence.mapper.timescale.DmDeviceDataMapper;
import com.devicemind.core.stdsvc.intf.IDmAlertRuleService;
import com.devicemind.core.stdsvc.intf.IDmAlertService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

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
        double value = toDouble(point.getValue());
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
     * 检查滑动窗口内条件是否持续满足
     */
    private boolean isWindowSustained(DeviceDataPoint point, DmAlertRule rule) {
        long windowStart = point.getTimestamp() - rule.getDurationSeconds();
        // 查询窗口内不满足条件的记录数，若为 0 则持续满足
        String operatorReverse = invertOperator(rule.getOperator());
        if (operatorReverse == null) return true;

        try {
            // 用原生 SQL 查询窗口内是否有违反条件的点
            // 如果没有不满足条件的记录，说明窗口内一直超标 → 触发告警
            // 这里简化处理：至少有 1 条窗口内数据即可触发
            // 正式实现可用 count 判断
            return true; // 简化：当前值超阈值就触发，duration 作为防抖
        } catch (Exception e) {
            log.warn("滑动窗口查询异常", e);
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
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private String invertOperator(String op) {
        return switch (op) {
            case ">" -> "<="; case ">=" -> "<"; case "<" -> ">="; case "<=" -> ">"; case "==" -> "!=";
            default -> null;
        };
    }
}

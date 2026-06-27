package com.devicemind.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.model.entity.DmSceneLog;
import com.devicemind.core.stdsvc.intf.IDmAlertService;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import com.devicemind.core.stdsvc.intf.IDmSceneLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 数据清理服务 — 定时清理过期数据，防止表无限增长
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final IDmSceneLogService sceneLogService;
    private final IDmCommandLogService commandLogService;
    private final IDmAlertService alertService;

    /** 场景日志保留天数 */
    private static final int SCENE_LOG_RETENTION_DAYS = 30;
    /** 指令日志保留天数 */
    private static final int COMMAND_LOG_RETENTION_DAYS = 30;
    /** 已恢复告警保留天数 */
    private static final int RESOLVED_ALERT_RETENTION_DAYS = 7;

    /**
     * 每天凌晨2点执行清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanup() {
        log.info("开始清理过期数据...");
        cleanupSceneLogs();
        cleanupCommandLogs();
        cleanupResolvedAlerts();
        log.info("过期数据清理完成");
    }

    private void cleanupSceneLogs() {
        Date before = daysAgo(SCENE_LOG_RETENTION_DAYS);
        long count = sceneLogService.lambdaQuery()
                .lt(DmSceneLog::getCreatedDate, before)
                .count();
        if (count > 0) {
            sceneLogService.lambdaQuery()
                    .lt(DmSceneLog::getCreatedDate, before)
                    .remove();
            log.info("清理场景日志: {} 条 (保留{}天)", count, SCENE_LOG_RETENTION_DAYS);
        }
    }

    private void cleanupCommandLogs() {
        Date before = daysAgo(COMMAND_LOG_RETENTION_DAYS);
        long count = commandLogService.lambdaQuery()
                .lt(DmCommandLog::getCreatedDate, before)
                .count();
        if (count > 0) {
            commandLogService.lambdaQuery()
                    .lt(DmCommandLog::getCreatedDate, before)
                    .remove();
            log.info("清理指令日志: {} 条 (保留{}天)", count, COMMAND_LOG_RETENTION_DAYS);
        }
    }

    private void cleanupResolvedAlerts() {
        Date before = daysAgo(RESOLVED_ALERT_RETENTION_DAYS);
        long count = alertService.lambdaQuery()
                .in(DmAlert::getStatus, "RESOLVED", "CONFIRMED")
                .lt(DmAlert::getUpdatedDate, before)
                .count();
        if (count > 0) {
            alertService.lambdaQuery()
                    .in(DmAlert::getStatus, "RESOLVED", "CONFIRMED")
                    .lt(DmAlert::getUpdatedDate, before)
                    .remove();
            log.info("清理已恢复告警: {} 条 (保留{}天)", count, RESOLVED_ALERT_RETENTION_DAYS);
        }
    }

    private static Date daysAgo(int days) {
        return Date.from(LocalDateTime.now().minusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant());
    }
}

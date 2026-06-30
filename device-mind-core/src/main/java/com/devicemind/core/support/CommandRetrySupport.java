package com.devicemind.core.support;

import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import com.devicemind.common.kafka.producer.DeviceCommandProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 指令重试服务
 * <p>
 * 定时重试 PENDING/SENT 状态的指令，设备上线时亦会触发投递。
 */
@Slf4j
@Service
public class CommandRetrySupport {

    @Autowired
    private IDmCommandLogService commandLogService;
    @Autowired
    private DeviceCommandProducer deviceCommandProducer;
    /** 最大重试次数 */
    private static final int MAX_RETRIES = 5;

    /**
     * 每30秒扫描一次待发送指令并重试
     */
    @Scheduled(fixedDelay = 30000)
    public void retryPendingCommands() {
        List<DmCommandLog> pending = commandLogService.lambdaQuery()
                .in(DmCommandLog::getStatus, "PENDING", "SENT")
                .lt(DmCommandLog::getRetryCount, MAX_RETRIES)
                .list();

        if (pending.isEmpty()) return;
        log.debug("重试扫描: 待重试指令 {} 条", pending.size());

        for (DmCommandLog cmd : pending) {
            try {
                Map<String, Object> params = null;
                if (cmd.getParams() != null && !cmd.getParams().isBlank()) {
                    params = JsonUtil.fromJson(cmd.getParams(),
                            new TypeReference<Map<String, Object>>() {});
                }

                String paramsJson = params != null && !params.isEmpty()
                        ? JsonUtil.toJson(params) : "{}";
                deviceCommandProducer.sendCommandAsync(
                        cmd.getDeviceId(), cmd.getCommand(), paramsJson, cmd.getIdempotencyKey());

                // Kafka 发送成功 → 标记 SENT，等设备响应后由 DeviceResponseConsumer 改为 SUCCESS
                cmd.setStatus("SENT");
                cmd.setRetryCount(cmd.getRetryCount() + 1);
                commandLogService.updateById(cmd);
                log.info("指令已发送到 Kafka: id={}, deviceId={}, command={}",
                        cmd.getId(), cmd.getDeviceId(), cmd.getCommand());
            } catch (Exception e) {
                log.warn("指令重试异常: id={}", cmd.getId(), e);
            }
        }
    }

    /**
     * 设备上线时投递待发送指令
     */
    public void deliverPendingCommands(String deviceId) {
        List<DmCommandLog> pending = commandLogService.lambdaQuery()
                .eq(DmCommandLog::getDeviceId, deviceId)
                .in(DmCommandLog::getStatus, "PENDING", "SENT")
                .list();

        if (pending.isEmpty()) return;
        log.info("设备上线，投递待发送指令: deviceId={}, 数量={}", deviceId, pending.size());

        for (DmCommandLog cmd : pending) {
            try {
                Map<String, Object> params = null;
                if (cmd.getParams() != null && !cmd.getParams().isBlank()) {
                    params = JsonUtil.fromJson(cmd.getParams(),
                            new TypeReference<Map<String, Object>>() {});
                }

                String paramsJson = params != null && !params.isEmpty()
                        ? JsonUtil.toJson(params) : "{}";
                deviceCommandProducer.sendCommandAsync(
                        cmd.getDeviceId(), cmd.getCommand(), paramsJson, cmd.getIdempotencyKey());

                // 标记 SENT，等设备响应后由 DeviceResponseConsumer 改为 SUCCESS
                cmd.setStatus("SENT");
                cmd.setRetryCount(cmd.getRetryCount() + 1);
                commandLogService.updateById(cmd);
                log.info("上线指令投递成功: id={}, deviceId={}, command={}",
                        cmd.getId(), deviceId, cmd.getCommand());
            } catch (Exception e) {
                log.warn("上线投递异常: id={}, deviceId={}", cmd.getId(), deviceId, e);
            }
        }
    }
}

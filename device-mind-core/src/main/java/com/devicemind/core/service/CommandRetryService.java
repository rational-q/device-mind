package com.devicemind.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import com.devicemind.core.client.BrokerCommandClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CommandRetryService {

    private final IDmCommandLogService commandLogService;
    private final BrokerCommandClient brokerCommandClient;
    private final ObjectMapper objectMapper;

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
                    params = objectMapper.readValue(cmd.getParams(),
                            new TypeReference<Map<String, Object>>() {});
                }

                boolean sent = brokerCommandClient.sendCommand(
                        cmd.getDeviceId(), cmd.getCommand(), params, cmd.getIdempotencyKey());

                if (sent) {
                    cmd.setStatus("SENT");
                    cmd.setRetryCount(cmd.getRetryCount() + 1);
                    commandLogService.updateById(cmd);
                    log.info("指令重试发送成功: id={}, deviceId={}, command={}",
                            cmd.getId(), cmd.getDeviceId(), cmd.getCommand());
                } else {
                    cmd.setRetryCount(cmd.getRetryCount() + 1);
                    if (cmd.getRetryCount() >= MAX_RETRIES) {
                        cmd.setStatus("EXPIRED");
                        log.warn("指令重试达上限，标记过期: id={}, deviceId={}", cmd.getId(), cmd.getDeviceId());
                    }
                    commandLogService.updateById(cmd);
                }
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
                    params = objectMapper.readValue(cmd.getParams(),
                            new TypeReference<Map<String, Object>>() {});
                }

                boolean sent = brokerCommandClient.sendCommand(
                        cmd.getDeviceId(), cmd.getCommand(), params, cmd.getIdempotencyKey());

                if (sent) {
                    cmd.setStatus("SENT");
                    cmd.setRetryCount(cmd.getRetryCount() + 1);
                    commandLogService.updateById(cmd);
                }
            } catch (Exception e) {
                log.warn("上线投递异常: id={}, deviceId={}", cmd.getId(), deviceId, e);
            }
        }
    }
}

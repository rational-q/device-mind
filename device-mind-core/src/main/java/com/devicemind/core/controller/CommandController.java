package com.devicemind.core.controller;

import com.devicemind.common.kafka.producer.DeviceCommandProducer;
import com.devicemind.common.utils.Result;
import com.devicemind.core.model.dto.CommandSendDTO;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import com.devicemind.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 指令下发 REST API
 * <p>
 * 接收前端/Agent 的指令下发请求：先落库为 PENDING，再通过 Kafka 投递到
 * Broker → MQTT → 设备。Kafka 发送成功置为 SENT，设备回执后由
 * DeviceResponseConsumer 置为 SUCCESS；失败或未回执的由 CommandRetrySupport 兜底重试。
 */
@Slf4j
@RestController
@RequestMapping("/commands")
@Tag(name = "指令下发", description = "向设备下发控制指令")
public class CommandController {

    @Autowired
    private DeviceCommandProducer deviceCommandProducer;
    @Autowired
    private IDmCommandLogService commandLogService;

    @PostMapping("/send")
    @Operation(summary = "下发设备指令", description = "先落库再通过 Kafka 向设备下发控制指令")
    public Result<Map<String, Object>> sendCommand(@Valid @RequestBody CommandSendDTO request) {
        String deviceId = request.getDeviceId();
        String command = request.getCommand();
        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey() : UUID.randomUUID().toString();

        String paramsJson;
        try {
            Map<String, Object> params = request.getParams();
            paramsJson = params != null && !params.isEmpty()
                    ? JsonUtil.toJson(params) : "{}";
        } catch (Exception e) {
            log.error("指令参数序列化失败: deviceId={}, command={}", deviceId, command, e);
            return Result.fail("参数序列化失败: " + e.getMessage());
        }

        // 1. 幂等：同 idempotencyKey 已存在则直接返回，不重复下发
        DmCommandLog existing = commandLogService.lambdaQuery()
                .eq(DmCommandLog::getIdempotencyKey, idempotencyKey)
                .one();
        if (existing != null) {
            log.info("指令幂等命中，跳过重复下发: idempotencyKey={}", idempotencyKey);
            return Result.ok(Map.of(
                    "idempotencyKey", idempotencyKey,
                    "deviceId", deviceId,
                    "command", command,
                    "message", "指令已存在（幂等），当前状态: " + existing.getStatus()
            ));
        }

        // 2. 先落库为 PENDING（保证可观测 + 可重试）
        DmCommandLog cmdLog = new DmCommandLog();
        cmdLog.setDeviceId(deviceId);
        cmdLog.setCommand(command);
        cmdLog.setParams(paramsJson);
        cmdLog.setIdempotencyKey(idempotencyKey);
        cmdLog.setStatus("PENDING");
        cmdLog.setRetryCount(0);
        cmdLog.setMaxRetries(5);
        try {
            commandLogService.save(cmdLog);
        } catch (Exception e) {
            log.error("指令落库失败: deviceId={}, command={}", deviceId, command, e);
            return Result.fail("指令落库失败: " + e.getMessage());
        }

        // 3. 投递 Kafka；成功置 SENT，失败保留 PENDING 由重试任务兜底
        try {
            deviceCommandProducer.sendCommandAsync(deviceId, command, paramsJson, idempotencyKey);
            cmdLog.setStatus("SENT");
            cmdLog.setRetryCount(1);
            commandLogService.updateById(cmdLog);
            log.info("指令已发送到 Kafka: deviceId={}, command={}, key={}", deviceId, command, idempotencyKey);

            return Result.ok(Map.of(
                    "idempotencyKey", idempotencyKey,
                    "deviceId", deviceId,
                    "command", command,
                    "message", "指令已发送，等待设备执行"
            ));
        } catch (Exception e) {
            log.error("指令下发失败（已落库 PENDING，将由重试任务兜底）: deviceId={}, command={}", deviceId, command, e);
            return Result.ok(Map.of(
                    "idempotencyKey", idempotencyKey,
                    "deviceId", deviceId,
                    "command", command,
                    "message", "指令已受理，Kafka 投递失败将自动重试"
            ));
        }
    }
}

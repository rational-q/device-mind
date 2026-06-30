package com.devicemind.core.controller;

import com.devicemind.common.kafka.producer.DeviceCommandProducer;
import com.devicemind.common.utils.Result;
import com.devicemind.core.model.dto.CommandSendDTO;
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
 * 接收前端/Agent 的指令下发请求，通过 Kafka 投递到 Broker → MQTT → 设备。
 */
@Slf4j
@RestController
@RequestMapping("/commands")
@Tag(name = "指令下发", description = "向设备下发控制指令")
public class CommandController {

    @Autowired
    private DeviceCommandProducer deviceCommandProducer;
    @PostMapping("/send")
    @Operation(summary = "下发设备指令", description = "通过 Kafka 向设备下发控制指令，设备需在线")
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

        try {
            deviceCommandProducer.sendCommandAsync(deviceId, command, paramsJson, idempotencyKey);
            log.info("指令已发送到 Kafka: deviceId={}, command={}, key={}", deviceId, command, idempotencyKey);

            return Result.ok(Map.of(
                    "idempotencyKey", idempotencyKey,
                    "deviceId", deviceId,
                    "command", command,
                    "message", "指令已发送，等待设备执行"
            ));
        } catch (Exception e) {
            log.error("指令下发失败: deviceId={}, command={}", deviceId, command, e);
            return Result.fail("指令下发失败: " + e.getMessage());
        }
    }
}

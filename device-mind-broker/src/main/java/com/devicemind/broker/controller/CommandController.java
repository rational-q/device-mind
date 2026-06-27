package com.devicemind.broker.controller;

import com.devicemind.broker.dto.CommandRequest;
import com.devicemind.broker.dto.CommandResponse;
import com.devicemind.broker.service.CommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 指令下发 REST API — 供 Core 模块调用
 * <p>
 * 监听端口 1884（server.port），非 MQTT 端口。
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{deviceId}/command")
    public CommandResponse sendCommand(@PathVariable String deviceId,
                                       @RequestBody CommandRequest request) {
        log.info("收到指令下发请求: deviceId={}, command={}", deviceId, request.getCommand());

        String topic = "device/command/" + deviceId;
        String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("序列化指令失败", e);
            return new CommandResponse(false, "指令序列化失败");
        }

        boolean sent = commandService.sendCommand(deviceId, topic, payload);
        if (sent) {
            return new CommandResponse(true, "指令已发送");
        } else {
            return new CommandResponse(false, "设备离线");
        }
    }
}

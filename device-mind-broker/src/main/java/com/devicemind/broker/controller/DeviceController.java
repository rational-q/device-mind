package com.devicemind.broker.controller;

import com.devicemind.broker.service.DeviceAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 设备注册管理 REST API — 供 Core 模块调用
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceAuthService deviceAuthService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String deviceId = body.get("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            return Map.of("success", false, "message", "deviceId 不能为空");
        }
        deviceAuthService.register(deviceId);
        log.info("设备注册成功: deviceId={}", deviceId);
        return Map.of("success", true, "message", "已注册");
    }

    @PostMapping("/unregister")
    public Map<String, Object> unregister(@RequestBody Map<String, String> body) {
        String deviceId = body.get("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            return Map.of("success", false, "message", "deviceId 不能为空");
        }
        deviceAuthService.unregister(deviceId);
        log.info("设备注销成功: deviceId={}", deviceId);
        return Map.of("success", true, "message", "已注销");
    }

    @GetMapping("/check")
    public Map<String, Object> check(@RequestParam String deviceId) {
        boolean registered = deviceAuthService.isRegistered(deviceId);
        return Map.of("success", true, "registered", registered);
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        return Map.of("success", true, "devices", deviceAuthService.getAllDeviceIds(), "count", deviceAuthService.getRegisteredCount());
    }
}

package com.devicemind.broker.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备认证服务 — 维护已注册设备白名单
 * <p>
 * 启动时从 Core REST 拉取全量设备列表，运行中通过 Kafka device-lifecycle 增删。
 */
@Slf4j
@Service
public class DeviceAuthService {

    private final RestTemplate restTemplate;

    /** 已注册的设备ID集合 */
    private final Set<String> registeredDevices = ConcurrentHashMap.newKeySet();

    @Value("${core-service.url:http://localhost:8080}")
    private String coreUrl;

    public DeviceAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 启动时从 Core 同步全量设备列表（带重试 + 指数退避）
     */
    @PostConstruct
    public void syncFromCore() {
        int maxRetries = 5;
        long backoffMs = 1000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("从 Core 同步设备列表（第 {} 次尝试）...", attempt);
                doSync();
                log.info("设备列表同步完成，共 {} 台设备", registeredDevices.size());
                return;
            } catch (Exception e) {
                log.warn("从 Core 同步设备列表失败（{}/{}）: {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("从 Core 同步设备列表最终失败，设备认证功能受限");
    }

    private void doSync() {
        int page = 1;
        int total;
        do {
            Map<String, Object> body = Map.of("pageNum", page, "pageSize", 1000);
            Map<String, Object> resp = restTemplate.postForObject(
                    coreUrl + "/device-mind/core/devices/list", body, Map.class);
            if (resp == null) break;

            Object recordsObj = resp.get("records");
            if (recordsObj instanceof List<?> records) {
                for (Object r : records) {
                    if (r instanceof Map<?, ?> device) {
                        Object deviceId = device.get("deviceId");
                        if (deviceId != null) {
                            registeredDevices.add(deviceId.toString());
                        }
                    }
                }
                total = records.size();
                page++;
            } else {
                break;
            }
        } while (total >= 1000);
    }

    /** 注册设备（由 Kafka device-lifecycle 消费者调用） */
    public void register(String deviceId) {
        registeredDevices.add(deviceId);
        log.debug("设备已注册: deviceId={}, 总数={}", deviceId, registeredDevices.size());
    }

    /** 注销设备（由 Kafka device-lifecycle 消费者调用） */
    public void unregister(String deviceId) {
        registeredDevices.remove(deviceId);
        log.debug("设备已注销: deviceId={}, 总数={}", deviceId, registeredDevices.size());
    }

    /** 检查设备是否已注册 */
    public boolean isRegistered(String deviceId) {
        return registeredDevices.contains(deviceId);
    }

    /** 获取已注册设备数 */
    public int getRegisteredCount() {
        return registeredDevices.size();
    }

    /** 获取所有已注册设备ID */
    public List<String> getAllDeviceIds() {
        return List.copyOf(registeredDevices);
    }
}

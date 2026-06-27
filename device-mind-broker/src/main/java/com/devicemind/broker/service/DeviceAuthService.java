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
 * 启动时从 Core 拉取全量设备列表，运行中通过 Core 通知增删。
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
     * 启动时从 Core 同步全量设备列表
     */
    @PostConstruct
    public void syncFromCore() {
        try {
            log.info("启动时从 Core 同步设备列表...");
            // Core 的 list 接口返回分页数据，循环拉取所有设备
            int page = 1;
            int total = 0;
            do {
                Map<String, Object> body = Map.of("pageNum", page, "pageSize", 1000);
                Map<String, Object> resp = restTemplate.postForObject(
                        coreUrl + "/device-mind/devices/list", body, Map.class);
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

            log.info("设备列表同步完成，共 {} 台设备", registeredDevices.size());
        } catch (Exception e) {
            log.warn("从 Core 同步设备列表失败，设备认证功能受限: {}", e.getMessage());
        }
    }

    /** 注册设备 */
    public void register(String deviceId) {
        registeredDevices.add(deviceId);
        log.debug("设备已注册: deviceId={}, 总数={}", deviceId, registeredDevices.size());
    }

    /** 注销设备 */
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

    /** 批量注册（用于Core同步） */
    public void registerAll(List<String> deviceIds) {
        registeredDevices.addAll(deviceIds);
    }

    /**
     * 通知 Core 更新设备在线状态
     */
    public void notifyStatusChange(String deviceId, String status) {
        try {
            String url = coreUrl + "/device-mind/devices/online-status?deviceId=" + deviceId + "&status=" + status;
            restTemplate.put(url, null);
            log.debug("已通知 Core 更新设备状态: deviceId={}, status={}", deviceId, status);
        } catch (Exception e) {
            log.warn("通知 Core 更新状态失败: deviceId={}, status={}", deviceId, status, e);
        }
    }
}

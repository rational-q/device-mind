package com.devicemind.core.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Broker 设备注册客户端 — 通知 Broker 设备注册/注销
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerRegistryClient {

    private final RestTemplate restTemplate;

    @Value("${broker-service.url:http://localhost:1884}")
    private String brokerUrl;

    /** 通知 Broker 注册设备 */
    public boolean register(String deviceId) {
        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    brokerUrl + "/api/device/register",
                    Map.of("deviceId", deviceId),
                    Map.class);
            boolean success = resp != null && Boolean.TRUE.equals(resp.get("success"));
            log.info("Broker 注册结果: deviceId={}, success={}", deviceId, success);
            return success;
        } catch (Exception e) {
            log.warn("通知 Broker 注册失败: deviceId={}", deviceId, e);
            return false;
        }
    }

    /** 通知 Broker 注销设备 */
    public boolean unregister(String deviceId) {
        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    brokerUrl + "/api/device/unregister",
                    Map.of("deviceId", deviceId),
                    Map.class);
            boolean success = resp != null && Boolean.TRUE.equals(resp.get("success"));
            log.info("Broker 注销结果: deviceId={}, success={}", deviceId, success);
            return success;
        } catch (Exception e) {
            log.warn("通知 Broker 注销失败: deviceId={}", deviceId, e);
            return false;
        }
    }
}

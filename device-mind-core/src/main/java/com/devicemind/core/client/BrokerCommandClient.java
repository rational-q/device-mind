package com.devicemind.core.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Broker 指令下发客户端 — 调用 Broker REST API 给设备发 MQTT 指令
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerCommandClient {

    private final RestTemplate restTemplate;

    @Value("${broker-service.url:http://localhost:1884}")
    private String brokerUrl;

    /**
     * 向设备下发指令
     *
     * @param deviceId 目标设备ID
     * @param command  指令名称
     * @param params   指令参数
     * @param idempotencyKey 幂等键
     * @return true 表示发送成功
     */
    @SuppressWarnings("unchecked")
    public boolean sendCommand(String deviceId, String command, Map<String, Object> params,
                               String idempotencyKey) {
        try {
            String url = brokerUrl + "/api/device/" + deviceId + "/command";
            Map<String, Object> body = Map.of(
                    "command", command,
                    "params", params != null ? params : Map.of(),
                    "idempotencyKey", idempotencyKey != null ? idempotencyKey : ""
            );
            Map<String, Object> resp = restTemplate.postForObject(url, body, Map.class);
            boolean success = resp != null && Boolean.TRUE.equals(resp.get("success"));
            log.info("指令下发结果: deviceId={}, command={}, success={}", deviceId, command, success);
            return success;
        } catch (Exception e) {
            log.error("调用 Broker 指令下发失败: deviceId={}, command={}", deviceId, command, e);
            return false;
        }
    }
}

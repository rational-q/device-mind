package com.devicemind.core.client;

import com.devicemind.core.model.entity.DmAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Agent 告警分析客户端
 * <p>
 * 告警触发后异步调用 Agent 的 AI 分析接口，结果回写到 dm_alert.ai_analysis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertAnalysisClient {

    private final RestTemplate restTemplate;

    @Value("${agent-service.url:http://localhost:8081}")
    private String agentUrl;

    /**
     * 调用 Agent 分析告警
     *
     * @param alert 告警实体
     * @return AI 分析结果 JSON，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public String analyze(DmAlert alert) {
        try {
            Map<String, Object> body = Map.of(
                    "deviceId", alert.getDeviceId(),
                    "ruleName", alert.getRuleName() != null ? alert.getRuleName() : "",
                    "level", alert.getLevel() != null ? alert.getLevel() : "",
                    "metric", alert.getMetric() != null ? alert.getMetric() : "",
                    "currentValue", alert.getCurrentValue() != null ? alert.getCurrentValue() : 0,
                    "threshold", alert.getThreshold() != null ? alert.getThreshold() : 0
            );

            Map<String, Object> resp = restTemplate.postForObject(
                    agentUrl + "/api/v1/analysis/alert",
                    body,
                    Map.class);

            if (resp != null && Boolean.TRUE.equals(resp.get("success"))) {
                StringBuilder sb = new StringBuilder();
                if (resp.get("summary") != null) {
                    sb.append("【分析结论】").append(resp.get("summary")).append("\n");
                }
                if (resp.get("severity") != null) {
                    sb.append("【严重程度】").append(resp.get("severity")).append("\n");
                }
                if (resp.get("possibleCauses") != null) {
                    sb.append("【可能原因】").append(resp.get("possibleCauses")).append("\n");
                }
                if (resp.get("recommendations") != null) {
                    sb.append("【处理建议】").append(resp.get("recommendations"));
                }
                String result = sb.toString();
                log.info("告警 AI 分析完成: alertId={}, summary={}", alert.getId(), resp.get("summary"));
                return result;
            } else {
                log.warn("告警 AI 分析返回失败: alertId={}, resp={}", alert.getId(), resp);
                return null;
            }
        } catch (Exception e) {
            log.warn("调用 Agent 分析异常: alertId={}", alert.getId(), e);
            return null;
        }
    }
}

package com.devicemind.agent.function.handler;

import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.agent.client.CoreApiClient;
import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.devicemind.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlertHistoryHandler implements FunctionHandler {

    @Autowired
    private CoreApiClient coreApiClient;
    @Override
    public String getFunctionName() {
        return "alertHistory";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("alertHistory")
                        .description("查询设备最近N小时的告警历史记录，包括告警等级、规则名称、触发时间等")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("设备ID，例如 A-102")
                                        .build())
                                .property("hours", ToolDefinition.ParameterProperty.builder()
                                        .type("number")
                                        .description("查询最近N小时的告警，默认24小时")
                                        .build())
                                .required(List.of("deviceId"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = JsonUtil.readTree(argumentsJson);
            String deviceId = args.get("deviceId").asText();
            Integer hours = args.has("hours") && !args.get("hours").isNull()
                    ? args.get("hours").asInt() : null;
            return coreApiClient.getAlertHistory(deviceId, hours);
        } catch (Exception e) {
            return "{\"error\":\"参数解析失败: " + e.getMessage() + "\"}";
        }
    }
}

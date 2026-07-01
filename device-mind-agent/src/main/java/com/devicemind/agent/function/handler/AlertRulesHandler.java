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
public class AlertRulesHandler implements FunctionHandler {

    @Autowired
    private CoreApiClient coreApiClient;
    @Override
    public String getFunctionName() {
        return "alertRules";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("alertRules")
                        .description("查询告警规则列表，可按产品类型过滤，返回规则的阈值、运算符、持续时长等配置")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceType", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("产品类型标识（可选），例如 TEMP_SENSOR_V1、SMART_LOCK_V1")
                                        .build())
                                .required(List.of())
                                .build())
                        .build())
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = JsonUtil.readTree(argumentsJson);
            String deviceType = args.has("deviceType") && !args.get("deviceType").isNull()
                    ? args.get("deviceType").asText() : null;
            return coreApiClient.getAlertRules(deviceType);
        } catch (Exception e) {
            return com.devicemind.agent.function.FunctionHandler.errorJson("参数解析失败: " + e.getMessage());
        }
    }
}

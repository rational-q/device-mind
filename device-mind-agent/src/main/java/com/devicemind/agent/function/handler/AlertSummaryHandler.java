package com.devicemind.agent.function.handler;

import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.agent.client.CoreApiClient;
import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.devicemind.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 告警概览统计工具
 * <p>
 * 查询近 N 小时的告警概览：总数、按严重等级（CRITICAL/WARN/INFO）分布。
 */
@Component
public class AlertSummaryHandler implements FunctionHandler {

    @Autowired
    private CoreApiClient coreApiClient;
    @Override
    public String getFunctionName() {
        return "alertSummary";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("alertSummary")
                        .description("查询最近N小时的告警概览统计：返回告警总数、按等级（CRITICAL/WARN/INFO）分布。可用于回答'最近有哪些告警''严重告警有多少'等问题")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("hours", ToolDefinition.ParameterProperty.builder()
                                        .type("number")
                                        .description("查询最近N小时，默认1小时")
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
            Integer hours = args.has("hours") && !args.get("hours").isNull()
                    ? args.get("hours").asInt() : null;
            return coreApiClient.getAlertSummary(hours);
        } catch (Exception e) {
            return "{\"error\":\"参数解析失败: " + e.getMessage() + "\"}";
        }
    }
}

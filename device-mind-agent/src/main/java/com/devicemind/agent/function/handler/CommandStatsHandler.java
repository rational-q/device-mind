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
 * 指令执行统计工具
 * <p>
 * 查询指令下发成功率：总数、成功数、失败数、执行中数。
 */
@Component
public class CommandStatsHandler implements FunctionHandler {

    @Autowired
    private CoreApiClient coreApiClient;
    @Override
    public String getFunctionName() {
        return "commandStats";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("commandStats")
                        .description("查询指令下发执行统计：返回总数、成功数、失败数、待执行数、成功率。可按设备ID和时间范围过滤。可用于回答'指令下发成功率多少''最近下发过哪些指令'等问题")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("设备ID（可选），查询指定设备的指令统计")
                                        .build())
                                .property("hours", ToolDefinition.ParameterProperty.builder()
                                        .type("number")
                                        .description("查询最近N小时，默认24小时")
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
            String deviceId = args.has("deviceId") && !args.get("deviceId").isNull()
                    ? args.get("deviceId").asText() : null;
            Integer hours = args.has("hours") && !args.get("hours").isNull()
                    ? args.get("hours").asInt() : null;
            return coreApiClient.getCommandStats(deviceId, hours);
        } catch (Exception e) {
            return "{\"error\":\"参数解析失败: " + e.getMessage() + "\"}";
        }
    }
}

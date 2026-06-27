package com.devicemind.agent.function.handler;

import com.devicemind.agent.client.CoreApiClient;
import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceShadowHandler implements FunctionHandler {

    private final CoreApiClient coreApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getFunctionName() {
        return "deviceShadow";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("deviceShadow")
                        .description("查询设备影子状态，包括设备最新上报的属性值(reported)和平台期望值(desired)")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("设备ID，例如 A-102")
                                        .build())
                                .required(List.of("deviceId"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String deviceId = args.get("deviceId").asText();
            return coreApiClient.getDeviceShadow(deviceId);
        } catch (Exception e) {
            return "{\"error\":\"参数解析失败: " + e.getMessage() + "\"}";
        }
    }
}

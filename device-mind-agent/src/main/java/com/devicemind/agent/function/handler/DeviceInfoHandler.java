package com.devicemind.agent.function.handler;

import com.devicemind.agent.client.CoreApiClient;
import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeviceInfoHandler implements FunctionHandler {

    private final CoreApiClient coreApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getFunctionName() {
        return "deviceInfo";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("deviceInfo")
                        .description("查询设备基本信息，包括设备名称、产品类型、在线状态、安装位置、固件版本等")
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
            return coreApiClient.getDeviceInfo(deviceId);
        } catch (Exception e) {
            return "{\"error\":\"参数解析失败: " + e.getMessage() + "\"}";
        }
    }
}

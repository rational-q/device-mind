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
public class DeviceDataHandler implements FunctionHandler {

    @Autowired
    private CoreApiClient coreApiClient;
    @Override
    public String getFunctionName() {
        return "deviceData";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("deviceData")
                        .description("查询设备最近N小时的时序数据，可按属性名过滤（如temperature、humidity），返回时间序列数值")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("设备ID，例如 A-102")
                                        .build())
                                .property("attrName", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("属性名（可选），例如 temperature、humidity、voltage")
                                        .build())
                                .property("hours", ToolDefinition.ParameterProperty.builder()
                                        .type("number")
                                        .description("查询最近N小时的数据，默认1小时")
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
            String attrName = args.has("attrName") && !args.get("attrName").isNull()
                    ? args.get("attrName").asText() : null;
            Integer hours = args.has("hours") && !args.get("hours").isNull()
                    ? args.get("hours").asInt() : null;
            return coreApiClient.getDeviceData(deviceId, attrName, hours);
        } catch (Exception e) {
            return com.devicemind.agent.function.FunctionHandler.errorJson("参数解析失败: " + e.getMessage());
        }
    }
}

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
 * 设备在线状态统计工具
 * <p>
 * 查询设备在线/离线数量，可按设备ID或产品类型过滤。
 */
@Component
public class DeviceStatusHandler implements FunctionHandler {

    @Autowired
    private CoreApiClient coreApiClient;
    @Override
    public String getFunctionName() {
        return "deviceStatus";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("deviceStatus")
                        .description("查询设备在线状态统计：返回在线数、离线数、设备总数。可用于回答'有多少台设备在线''XX产品在线情况'等问题")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("设备ID（可选），查询单个设备的状态")
                                        .build())
                                .property("productKey", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("产品类型（可选），按产品过滤，例如 TEMP_SENSOR_V1")
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
            String productKey = args.has("productKey") && !args.get("productKey").isNull()
                    ? args.get("productKey").asText() : null;
            return coreApiClient.getDeviceStatusSummary(deviceId, productKey);
        } catch (Exception e) {
            return com.devicemind.agent.function.FunctionHandler.errorJson("参数解析失败: " + e.getMessage());
        }
    }
}

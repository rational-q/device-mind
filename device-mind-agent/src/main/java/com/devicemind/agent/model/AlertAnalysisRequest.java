package com.devicemind.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "告警分析请求")
public class AlertAnalysisRequest {

    @Schema(description = "设备ID", example = "A-102")
    private String deviceId;

    @Schema(description = "产品类型", example = "TEMP_SENSOR_V1")
    private String productKey;

    @Schema(description = "产品名称", example = "温湿度传感器")
    private String productName;

    @Schema(description = "设备名称", example = "车间A-1号传感器")
    private String deviceName;

    @Schema(description = "安装位置", example = "3号车间A线")
    private String location;

    @Schema(description = "告警规则名称", example = "温度过高")
    private String ruleName;

    @Schema(description = "告警等级", example = "WARN")
    private String level;

    @Schema(description = "监控属性", example = "temperature")
    private String metric;

    @Schema(description = "当前值", example = "38.5")
    private Double currentValue;

    @Schema(description = "阈值", example = "35.0")
    private Double threshold;

    @Schema(description = "触发时间（epoch秒）")
    private Long triggeredAt;

    @Schema(description = "最近N条时序数据（属性名→值列表）", example = "{\"temperature\": [36.2, 37.1, 38.5]}")
    private Map<String, List<Double>> recentData;
}

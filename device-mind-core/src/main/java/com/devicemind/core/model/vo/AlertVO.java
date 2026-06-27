package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class AlertVO {

    @Schema(description = "告警ID")
    private Long id;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "告警等级", example = "CRITICAL")
    private String level;

    @Schema(description = "监控属性名")
    private String metric;

    @Schema(description = "当前值")
    private Double currentValue;

    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "触发时间")
    private Date triggeredAt;

    @Schema(description = "确认时间")
    private Date confirmedAt;

    @Schema(description = "恢复时间")
    private Date resolvedAt;

    @Schema(description = "状态", example = "TRIGGERED")
    private String status;

    @Schema(description = "AI分析结果")
    private String aiAnalysis;
}

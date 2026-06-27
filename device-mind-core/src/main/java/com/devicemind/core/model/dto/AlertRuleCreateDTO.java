package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertRuleCreateDTO {

    @NotBlank
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;
    @NotBlank
    @Schema(description = "适用产品标识", example = "TEMP_SENSOR_V1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceType;
    @NotBlank
    @Schema(description = "监控属性标识", example = "temperature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String attrName;
    @NotBlank
    @Schema(description = "比较运算符", example = ">", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operator;
    @NotNull
    @Schema(description = "阈值", example = "80.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double threshold;
    @Schema(description = "持续时间窗口（秒）", example = "60")
    private Integer durationSeconds = 60;
    @NotBlank
    @Schema(description = "告警等级", example = "WARN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String level;
    @Schema(description = "是否启用")
    private Boolean enabled = true;
}

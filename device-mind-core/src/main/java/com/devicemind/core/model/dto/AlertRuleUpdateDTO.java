package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AlertRuleUpdateDTO {
    @Schema(description = "规则名称")
    private String ruleName;
    @Schema(description = "适用产品标识")
    private String deviceType;
    @Schema(description = "监控属性标识")
    private String attrName;
    @Schema(description = "比较运算符")
    private String operator;
    @Schema(description = "阈值")
    private Double threshold;
    @Schema(description = "持续时间窗口（秒）")
    private Integer durationSeconds;
    @Schema(description = "告警等级")
    private String level;
    @Schema(description = "是否启用")
    private Boolean enabled;
}

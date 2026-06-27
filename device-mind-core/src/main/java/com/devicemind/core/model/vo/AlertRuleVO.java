package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class AlertRuleVO {

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "适用产品标识")
    private String deviceType;

    @Schema(description = "监控属性标识")
    private String attrName;

    @Schema(description = "比较运算符", example = ">")
    private String operator;

    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "持续时间窗口（秒）")
    private Integer durationSeconds;

    @Schema(description = "告警等级", example = "WARN")
    private String level;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private Date createdDate;
}

package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AlertPageQueryDTO {
    @Schema(description = "设备ID")
    private String deviceId;
    @Schema(description = "告警状态", example = "TRIGGERED")
    private String status;
    @Schema(description = "告警等级", example = "CRITICAL")
    private String level;
    @Schema(description = "开始时间（epoch毫秒）")
    private Long startTime;
    @Schema(description = "结束时间（epoch毫秒）")
    private Long endTime;
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}

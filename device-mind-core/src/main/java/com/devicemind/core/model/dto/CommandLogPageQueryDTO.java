package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CommandLogPageQueryDTO {
    @Schema(description = "设备ID")
    private String deviceId;
    @Schema(description = "指令标识")
    private String command;
    @Schema(description = "状态", example = "SENT")
    private String status;
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}

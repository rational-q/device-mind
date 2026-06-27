package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class CommandLogVO {

    @Schema(description = "指令记录ID")
    private Long id;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "指令标识")
    private String command;

    @Schema(description = "指令参数（JSON）")
    private String params;

    @Schema(description = "幂等键")
    private String idempotencyKey;

    @Schema(description = "状态", example = "SENT")
    private String status;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetries;

    @Schema(description = "确认时间")
    private Date ackedAt;

    @Schema(description = "创建时间")
    private Date createdDate;
}

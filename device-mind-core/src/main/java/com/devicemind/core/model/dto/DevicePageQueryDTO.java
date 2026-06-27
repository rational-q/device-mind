package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DevicePageQueryDTO {

    @Schema(description = "设备ID（模糊搜索）")
    private String deviceId;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "在线状态", example = "ONLINE")
    private String status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}

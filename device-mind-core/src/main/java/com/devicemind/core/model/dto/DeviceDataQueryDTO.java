package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceDataQueryDTO {
    @NotBlank
    @Schema(description = "设备ID", example = "A-102", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;
    @Schema(description = "属性名称", example = "temperature")
    private String attrName;
    @Schema(description = "开始时间（epoch秒）")
    private Long start;
    @Schema(description = "结束时间（epoch秒）")
    private Long end;
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    @Schema(description = "每页条数", example = "100")
    private Integer pageSize = 100;
}

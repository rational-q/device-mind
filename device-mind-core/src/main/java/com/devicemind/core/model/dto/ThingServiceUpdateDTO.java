package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThingServiceUpdateDTO {
    @Schema(description = "服务名称")
    private String name;
    @Schema(description = "调用类型")
    private String callType;
    @Schema(description = "服务描述")
    private String description;
}

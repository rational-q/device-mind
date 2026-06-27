package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThingEventUpdateDTO {
    @Schema(description = "事件名称")
    private String name;
    @Schema(description = "事件类型")
    private String type;
    @Schema(description = "事件描述")
    private String description;
}

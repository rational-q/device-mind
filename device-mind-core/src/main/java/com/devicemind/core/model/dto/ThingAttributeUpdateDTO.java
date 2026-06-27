package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThingAttributeUpdateDTO {

    @Schema(description = "属性名称")
    private String name;
    @Schema(description = "数据类型", example = "DOUBLE")
    private String dataType;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "访问模式", example = "R")
    private String accessMode;
    @Schema(description = "描述")
    private String description;
}

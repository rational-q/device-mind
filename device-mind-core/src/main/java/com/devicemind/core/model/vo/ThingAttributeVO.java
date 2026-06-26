package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThingAttributeVO {

    @Schema(description = "属性ID")
    private Long id;

    @Schema(description = "所属产品ID")
    private Long productId;

    @Schema(description = "属性标识", example = "temperature")
    private String identifier;

    @Schema(description = "属性名称", example = "温度")
    private String name;

    @Schema(description = "数据类型", example = "DOUBLE")
    private String dataType;

    @Schema(description = "单位", example = "℃")
    private String unit;

    @Schema(description = "访问模式", example = "R")
    private String accessMode;

    @Schema(description = "描述")
    private String description;
}

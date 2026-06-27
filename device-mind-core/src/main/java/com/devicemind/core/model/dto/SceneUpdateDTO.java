package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SceneUpdateDTO {

    @Schema(description = "场景名称")
    private String name;

    @Schema(description = "场景描述")
    private String description;

    @Schema(description = "触发条件JSON")
    private String conditions;

    @Schema(description = "执行动作JSON")
    private String actions;
}

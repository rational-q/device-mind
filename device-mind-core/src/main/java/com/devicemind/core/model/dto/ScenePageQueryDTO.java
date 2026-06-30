package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScenePageQueryDTO {
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}

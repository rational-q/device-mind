package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class SceneVO {

    @Schema(description = "场景ID")
    private Long id;

    @Schema(description = "场景名称")
    private String name;

    @Schema(description = "场景描述")
    private String description;

    @Schema(description = "关联产品ID")
    private Long productId;

    @Schema(description = "触发条件JSON")
    private String conditions;

    @Schema(description = "执行动作JSON")
    private String actions;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private Date createdDate;

    @Schema(description = "更新时间")
    private Date updatedDate;
}

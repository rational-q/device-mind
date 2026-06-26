package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThingEventVO {

    @Schema(description = "事件ID")
    private Long id;

    @Schema(description = "所属产品ID")
    private Long productId;

    @Schema(description = "事件标识")
    private String identifier;

    @Schema(description = "事件名称")
    private String name;

    @Schema(description = "事件类型", example = "ALERT")
    private String type;

    @Schema(description = "事件描述")
    private String description;
}

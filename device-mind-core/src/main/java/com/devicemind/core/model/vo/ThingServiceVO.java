package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ThingServiceVO {

    @Schema(description = "服务ID")
    private Long id;

    @Schema(description = "所属产品ID")
    private Long productId;

    @Schema(description = "服务标识")
    private String identifier;

    @Schema(description = "服务名称")
    private String name;

    @Schema(description = "调用类型", example = "ASYNC")
    private String callType;

    @Schema(description = "服务描述")
    private String description;

    @Schema(description = "服务参数列表")
    private List<ThingServiceParamVO> params;
}

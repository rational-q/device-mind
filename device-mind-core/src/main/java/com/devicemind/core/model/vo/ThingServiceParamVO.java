package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThingServiceParamVO {

    @Schema(description = "参数ID")
    private Long id;

    @Schema(description = "所属服务ID")
    private Long serviceId;

    @Schema(description = "参数标识")
    private String identifier;

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "参数描述")
    private String description;
}

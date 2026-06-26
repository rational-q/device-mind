package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProductPageQueryDTO {

    @Schema(description = "产品标识（模糊搜索）")
    private String productKey;

    @Schema(description = "产品名称（模糊搜索）")
    private String name;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}

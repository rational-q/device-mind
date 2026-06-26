package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class ProductVO {

    @Schema(description = "产品ID")
    private Long id;

    @Schema(description = "产品标识", example = "TEMP_SENSOR_V1")
    private String productKey;

    @Schema(description = "产品名称", example = "温湿度传感器")
    private String name;

    @Schema(description = "产品描述")
    private String description;

    @Schema(description = "协议类型", example = "MQTT")
    private String protocolType;

    @Schema(description = "数据格式", example = "JSON")
    private String dataFormat;

    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间")
    private Date createdDate;
}

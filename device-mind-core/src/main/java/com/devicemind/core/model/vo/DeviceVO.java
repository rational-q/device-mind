package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class DeviceVO {

    @Schema(description = "设备内部ID")
    private Long id;

    @Schema(description = "设备唯一标识", example = "A-102")
    private String deviceId;

    @Schema(description = "所属产品ID")
    private Long productId;

    @Schema(description = "产品名称", example = "温湿度传感器")
    private String productName;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "在线状态", example = "ONLINE")
    private String status;

    @Schema(description = "最后上线时间")
    private Date lastOnlineTime;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "注册时间")
    private Date createdDate;
}

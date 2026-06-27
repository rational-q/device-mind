package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class ShadowVO {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备上报的最新状态")
    private Map<String, Object> reported;

    @Schema(description = "平台期望状态")
    private Map<String, Object> desired;

    @Schema(description = "上报版本号")
    private Integer reportedVersion;

    @Schema(description = "期望版本号")
    private Integer desiredVersion;

    @Schema(description = "更新时间")
    private Date updatedDate;
}

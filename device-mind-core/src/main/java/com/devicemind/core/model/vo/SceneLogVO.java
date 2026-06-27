package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class SceneLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "场景ID")
    private Long sceneId;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "触发设备ID")
    private String deviceId;

    @Schema(description = "触发时间")
    private Date triggeredAt;

    @Schema(description = "动作执行结果JSON")
    private String actionsResult;

    @Schema(description = "执行状态")
    private String status;

    @Schema(description = "创建时间")
    private Date createdDate;
}

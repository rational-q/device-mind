package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 场景执行日志
 */
@Data
@Accessors(chain = true)
@TableName("dm_scene_log")
public class DmSceneLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("SCENE_ID")
    private Long sceneId;

    @TableField("SCENE_NAME")
    private String sceneName;

    @TableField("DEVICE_ID")
    private String deviceId;

    @TableField("TRIGGERED_AT")
    private Date triggeredAt;

    @TableField("ACTIONS_RESULT")
    private String actionsResult;

    @TableField("STATUS")
    private String status;

    @TableField("CREATED_DATE")
    private Date createdDate;
}

package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 设备影子实体 — 映射 dm_device_shadow 表
 * <p>
 * 主键为业务字段 DEVICE_ID，无统一审计字段，故不继承 BasePojo。
 */
@Data
@Accessors(chain = true)
@TableName("dm_device_shadow")
public class DmDeviceShadow {

    /** 设备ID */
    @TableId(value = "DEVICE_ID", type = IdType.INPUT)
    private String deviceId;

    /** 设备上报的最新状态 */
    @TableField("REPORTED")
    private String reported;

    /** 平台期望状态 */
    @TableField("DESIRED")
    private String desired;

    /** 上报版本号 */
    @TableField("REPORTED_VERSION")
    private Integer reportedVersion;

    /** 期望版本号 */
    @TableField("DESIRED_VERSION")
    private Integer desiredVersion;

    /** 更新时间 */
    @TableField("UPDATED_DATE")
    private Date updatedDate;

    /** 更新人ID */
    @TableField("UPDATED_BY")
    private Long updatedBy;
}

package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devicemind.common.model.entity.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 设备信息实体 — 映射 dm_device 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_device")
public class DmDevice extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备唯一标识 */
    @TableField("DEVICE_ID")
    private String deviceId;

    /** 所属产品ID */
    @TableField("PRODUCT_ID")
    private Long productId;

    /** 设备名称 */
    @TableField("NAME")
    private String name;

    /** 安装位置 */
    @TableField("LOCATION")
    private String location;

    /** 在线状态 */
    @TableField("STATUS")
    private String status;

    /** 最后上线时间 */
    @TableField("LAST_ONLINE_TIME")
    private Date lastOnlineTime;

    /** 固件版本 */
    @TableField("FIRMWARE_VERSION")
    private String firmwareVersion;

    /** 标签 */
    @TableField("TAGS")
    private String tags;
}

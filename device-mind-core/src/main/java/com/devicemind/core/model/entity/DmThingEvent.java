package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devicemind.common.model.entity.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 物模型-事件定义实体 — 映射 dm_thing_event 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_thing_event")
public class DmThingEvent extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属产品ID */
    @TableField("PRODUCT_ID")
    private Long productId;

    /** 事件标识 */
    @TableField("IDENTIFIER")
    private String identifier;

    /** 事件名称 */
    @TableField("NAME")
    private String name;

    /** 事件类型：INFO/ALERT/ERROR */
    @TableField("TYPE")
    private String type;

    /** 事件描述 */
    @TableField("DESCRIPTION")
    private String description;
}

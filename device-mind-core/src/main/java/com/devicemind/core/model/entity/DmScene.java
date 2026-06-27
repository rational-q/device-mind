package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devicemind.common.model.entity.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 场景联动定义
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("dm_scene")
public class DmScene extends BasePojo {

    private String name;

    private String description;

    /** 关联产品ID */
    @TableField("PRODUCT_ID")
    private Long productId;

    /** 触发条件 JSON */
    @TableField("CONDITIONS")
    private String conditions;

    /** 执行动作 JSON */
    @TableField("ACTIONS")
    private String actions;

    @TableField("ENABLED")
    private Boolean enabled;
}

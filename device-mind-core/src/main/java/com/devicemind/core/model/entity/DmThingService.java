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
 * 物模型-服务定义实体 — 映射 dm_thing_service 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_thing_service")
public class DmThingService extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属产品ID */
    @TableField("PRODUCT_ID")
    private Long productId;

    /** 服务标识 */
    @TableField("IDENTIFIER")
    private String identifier;

    /** 服务名称 */
    @TableField("NAME")
    private String name;

    /** 调用类型 */
    @TableField("CALL_TYPE")
    private String callType;

    /** 服务描述 */
    @TableField("DESCRIPTION")
    private String description;
}

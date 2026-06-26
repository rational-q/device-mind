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
 * 服务参数实体 — 映射 dm_thing_service_param 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_thing_service_param")
public class DmThingServiceParam extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属服务ID */
    @TableField("SERVICE_ID")
    private Long serviceId;

    /** 参数标识 */
    @TableField("IDENTIFIER")
    private String identifier;

    /** 参数名称 */
    @TableField("NAME")
    private String name;

    /** 数据类型 */
    @TableField("DATA_TYPE")
    private String dataType;

    /** 是否必填 */
    @TableField("REQUIRED")
    private Boolean required;

    /** 单位 */
    @TableField("UNIT")
    private String unit;

    /** 参数描述 */
    @TableField("DESCRIPTION")
    private String description;
}

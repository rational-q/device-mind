package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devicemind.common.model.entity.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_thing_attribute")
public class DmThingAttribute extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("PRODUCT_ID")
    private Long productId;

    @TableField("IDENTIFIER")
    private String identifier;

    @TableField("NAME")
    private String name;

    @TableField("DATA_TYPE")
    private String dataType;

    @TableField("UNIT")
    private String unit;

    @TableField("ACCESS_MODE")
    private String accessMode;

    @TableField("DESCRIPTION")
    private String description;
}

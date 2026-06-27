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
@TableName("dm_product")
public class DmProductDO extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("PRODUCT_KEY")
    private String productKey;

    @TableField("NAME")
    private String name;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("PROTOCOL_TYPE")
    private String protocolType;

    @TableField("DATA_FORMAT")
    private String dataFormat;

    @TableField("STATUS")
    private String status;
}

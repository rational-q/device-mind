package com.devicemind.common.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 公共基础实体
 * <p>
 * 提供统一的审计字段：创建时间、创建人、修改时间、修改人，
 * 通过 MyBatis-Plus MetaObjectHandler 自动填充。
 */
@Data
@Accessors(chain = true)
public class BasePojo {

    @TableField(value = "CREATED_DATE", fill = FieldFill.INSERT)
    private Date createdDate;

    @TableField(value = "CREATED_BY", fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(value = "UPDATED_DATE", fill = FieldFill.UPDATE)
    private Date updatedDate;

    @TableField(value = "UPDATED_BY", fill = FieldFill.UPDATE)
    private Long updatedBy;
}

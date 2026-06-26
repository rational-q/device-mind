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
 * 告警规则实体 — 映射 dm_alert_rule 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_alert_rule")
public class DmAlertRule extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 规则名称 */
    @TableField("RULE_NAME")
    private String ruleName;

    /** 适用产品标识 */
    @TableField("DEVICE_TYPE")
    private String deviceType;

    /** 监控属性标识 */
    @TableField("ATTR_NAME")
    private String attrName;

    /** 比较运算符 */
    @TableField("OPERATOR")
    private String operator;

    /** 阈值 */
    @TableField("THRESHOLD")
    private Double threshold;

    /** 持续时间窗口（秒） */
    @TableField("DURATION_SECONDS")
    private Integer durationSeconds;

    /** 告警等级 */
    @TableField("LEVEL")
    private String level;

    /** 是否启用 */
    @TableField("ENABLED")
    private Boolean enabled;
}

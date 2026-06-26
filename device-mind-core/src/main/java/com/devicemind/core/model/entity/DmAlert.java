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
 * 告警事件实体 — 映射 dm_alert 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_alert")
public class DmAlert extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备ID */
    @TableField("DEVICE_ID")
    private String deviceId;

    /** 规则ID */
    @TableField("RULE_ID")
    private Long ruleId;

    /** 规则名称 */
    @TableField("RULE_NAME")
    private String ruleName;

    /** 告警等级 */
    @TableField("LEVEL")
    private String level;

    /** 监控属性 */
    @TableField("METRIC")
    private String metric;

    /** 当前值 */
    @TableField("CURRENT_VALUE")
    private Double currentValue;

    /** 阈值 */
    @TableField("THRESHOLD")
    private Double threshold;

    /** 触发时间 */
    @TableField("TRIGGERED_AT")
    private Date triggeredAt;

    /** 确认时间 */
    @TableField("CONFIRMED_AT")
    private Date confirmedAt;

    /** 恢复时间 */
    @TableField("RESOLVED_AT")
    private Date resolvedAt;

    /** 状态 */
    @TableField("STATUS")
    private String status;

    /** AI分析结果 */
    @TableField("AI_ANALYSIS")
    private String aiAnalysis;
}

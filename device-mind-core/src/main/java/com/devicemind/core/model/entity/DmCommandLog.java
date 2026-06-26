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
 * 指令下发记录实体 — 映射 dm_command_log 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dm_command_log")
public class DmCommandLog extends BasePojo {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备ID */
    @TableField("DEVICE_ID")
    private String deviceId;

    /** 指令标识 */
    @TableField("COMMAND")
    private String command;

    /** 指令参数（JSON） */
    @TableField("PARAMS")
    private String params;

    /** 幂等键 */
    @TableField("IDEMPOTENCY_KEY")
    private String idempotencyKey;

    /** 状态 */
    @TableField("STATUS")
    private String status;

    /** 重试次数 */
    @TableField("RETRY_COUNT")
    private Integer retryCount;

    /** 最大重试次数 */
    @TableField("MAX_RETRIES")
    private Integer maxRetries;

    /** 确认时间 */
    @TableField("ACKED_AT")
    private Date ackedAt;
}

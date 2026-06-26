package com.devicemind.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 设备时序数据实体 — 映射 TimescaleDB DEVICE_DATA 表
 * <p>
 * 时序表无统一审计字段与主键，故不继承 BasePojo。
 */
@Data
@Accessors(chain = true)
@TableName("device_data")
public class DmDeviceData {

    /** 数据时间（epoch 秒） */
    @TableField("TIME")
    private Instant time;

    /** 设备ID */
    @TableField("DEVICE_ID")
    private String deviceId;

    /** 属性名称（temperature / humidity） */
    @TableField("ATTR_NAME")
    private String attrName;

    /** 数值 */
    @TableField("VALUE")
    private Double value;

    /**
     * 从数据点 DTO 构建实体（仅数值类型可写入 TimescaleDB）
     *
     * @return 实体对象，非数值类型返回 null
     */
    public static DmDeviceData from(String deviceId, String attrName, Object value, long timestamp) {
        Double doubleValue = toDouble(value);
        if (doubleValue == null) {
            return null;
        }
        return new DmDeviceData()
                .setTime(Instant.ofEpochSecond(timestamp))
                .setDeviceId(deviceId)
                .setAttrName(attrName)
                .setValue(doubleValue);
    }

    /**
     * 将 Object 值转换为 Double（数值类型直接转，字符串尝试解析）
     */
    private static Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

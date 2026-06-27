package com.devicemind.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 告警事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String ruleName;
    private String level;
    private double currentValue;
    private double threshold;
    private String trendData;
}

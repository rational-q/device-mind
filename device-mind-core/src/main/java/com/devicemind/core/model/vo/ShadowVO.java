package com.devicemind.core.model.vo;

import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data
public class ShadowVO {
    private String deviceId;
    private Map<String, Object> reported;
    private Map<String, Object> desired;
    private Integer reportedVersion;
    private Integer desiredVersion;
    private Date updatedDate;
}

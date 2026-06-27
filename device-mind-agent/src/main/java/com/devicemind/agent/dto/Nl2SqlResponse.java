package com.devicemind.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "NL2SQL 查询结果")
public class Nl2SqlResponse {

    @Schema(description = "分析是否成功")
    private boolean success;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "生成的SQL", example = "SELECT device_id, time, value FROM device_data WHERE attr_name = 'temperature' AND value > 30 AND time > NOW() - INTERVAL '1 hour'")
    private String sql;

    @Schema(description = "SQL说明")
    private String explanation;

    @Schema(description = "查询结果（列名→值列表）")
    private List<Map<String, Object>> results;

    @Schema(description = "数据量")
    private int resultCount;
}

package com.devicemind.agent.service;

import com.devicemind.agent.client.DeepSeekClient;
import com.devicemind.agent.dto.Nl2SqlRequest;
import com.devicemind.agent.dto.Nl2SqlResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL 自然语言查询服务
 * <p>
 * 将自然语言问题转换为 TimescaleDB (PostgreSQL) SQL 查询。
 * 包含完整的表结构上下文，确保生成的 SQL 语法正确。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlService {

    private final DeepSeekClient deepSeekClient;
    private final DataQueryService dataQueryService;
    private final ObjectMapper objectMapper;

    /** TimescaleDB 表结构定义 — 作为系统提示词的上下文 */
    private static final String SCHEMA_CONTEXT = """
            数据库: TimescaleDB (PostgreSQL 15)

            表结构:
            device_data (时序超表)
            - time           TIMESTAMPTZ    NOT NULL    — 数据时间戳
            - device_id      VARCHAR(64)    NOT NULL    — 设备唯一标识
            - attr_name      VARCHAR(50)    NOT NULL    — 属性名称（如 temperature, humidity）
            - value          DOUBLE PRECISION           — 属性值
            超表按 time 分区，按 device_id + time DESC 索引

            说明:
            1. 时间过滤使用 time > NOW() - INTERVAL 'N hours/minutes'
            2. 值比较用 value > N / value < N
            3. 支持 GROUP BY device_id 做聚合
            4. 所有查询必须使用 device_data 表名
            5. 返回结果建议限制 100 行
            6. 使用 PostgreSQL 语法，注意 INTERVAL 写法
            """;

    private static final String SYSTEM_PROMPT = """
            你是一个数据库查询助手，负责将自然语言转为 PostgreSQL / TimescaleDB SQL。

            %s

            ## 输出格式
            必须返回 JSON 格式（不要 markdown 代码块标记），字段如下：
            {
              "sql": "生成的完整 SQL 语句",
              "explanation": "这条 SQL 的含义说明"
            }

            仅返回 JSON，不要包含其他内容。
            如果问题不涉及数据查询或无法转换，sql 字段返回 null 并在 explanation 中说明原因。
            """.formatted(SCHEMA_CONTEXT);

    /**
     * 自然语言 → SQL 转换
     *
     * @param request 用户的自然语言查询
     * @return SQL + 说明
     */
    public Nl2SqlResponse generateSql(Nl2SqlRequest request) {
        String userMessage = buildUserPrompt(request);
        String rawResponse = deepSeekClient.chat(SYSTEM_PROMPT, userMessage);

        if (rawResponse == null) {
            return Nl2SqlResponse.builder()
                    .success(false)
                    .errorMsg("AI 服务暂时不可用（API Key 未配置或网络异常）")
                    .build();
        }

        try {
            // 清理可能的 markdown 包裹
            String json = rawResponse.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                if (json.endsWith("```")) json = json.substring(0, json.lastIndexOf("```"));
                json = json.trim();
            }

            Map<String, Object> result = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            String sql = getStringSafe(result, "sql");

            // 如果需要执行查询且 SQL 有效
            List<Map<String, Object>> queryResults = null;
            int resultCount = 0;
            if (request.isExecute() && sql != null && !sql.isBlank()
                    && !"null".equalsIgnoreCase(sql)) {
                log.info("执行 NL2SQL 查询: {}", sql);
                queryResults = dataQueryService.executeQuery(sql);
                resultCount = queryResults != null ? queryResults.size() : 0;
            }

            return Nl2SqlResponse.builder()
                    .success(true)
                    .sql(sql)
                    .explanation(getStringSafe(result, "explanation"))
                    .results(queryResults)
                    .resultCount(resultCount)
                    .rawResponse(rawResponse)
                    .build();

        } catch (Exception e) {
            log.warn("解析 NL2SQL 结果失败: {}", e.getMessage());
            // 降级：直接返回 AI 原始回复
            return Nl2SqlResponse.builder()
                    .success(true)
                    .sql(null)
                    .explanation(rawResponse)
                    .rawResponse(rawResponse)
                    .build();
        }
    }

    /** 构建用户消息 */
    private String buildUserPrompt(Nl2SqlRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请将以下自然语言查询转换为 SQL：\n\n");
        sb.append("查询: ").append(request.getQuestion()).append("\n");

        if (request.getDeviceId() != null) {
            sb.append("\n限定设备: ").append(request.getDeviceId()).append("\n");
        }
        if (request.getProductKey() != null) {
            sb.append("\n限定产品类型（用于关联 dm_product 表过滤）: ")
                    .append(request.getProductKey()).append("\n");
        }

        return sb.toString();
    }

    private String getStringSafe(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}

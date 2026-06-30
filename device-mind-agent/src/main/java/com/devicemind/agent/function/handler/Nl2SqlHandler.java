package com.devicemind.agent.function.handler;

import com.devicemind.agent.client.DeepSeekClient;
import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * NL2SQL 工具 — 将自然语言转为 SQL 并安全执行
 * <p>
 * 当 AI 判断 deviceData 工具覆盖不了时调用，如聚合统计、条件筛选、多属性对比等。
 * 收敛了原 Nl2SqlService 和 DataQueryService 的逻辑。
 */
@Slf4j
@Component
public class Nl2SqlHandler implements FunctionHandler {

    private static final int MAX_RESULT_ROWS = 200;

    private static final String SCHEMA_CONTEXT = """
            数据库: TimescaleDB (PostgreSQL 15)

            表结构:
            device_data (时序超表)
            - time           TIMESTAMPTZ        NOT NULL    — 数据时间戳
            - device_id      VARCHAR(64)        NOT NULL    — 设备唯一标识
            - attr_name      VARCHAR(50)        NOT NULL    — 属性名称（如 temperature, humidity）
            - value          DOUBLE PRECISION               — 数值型属性值（字符串型属性此列为 NULL）
            - value_text     TEXT                           — 字符串/枚举型属性值（数值型属性此列为 NULL）
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

    @Autowired
    private DeepSeekClient deepSeekClient;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public String getFunctionName() {
        return "nl2sql";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("nl2sql")
                        .description("""
                                将自然语言转为 SQL 查询 TimescaleDB 时序数据并返回结果。
                                适用于 deviceData 工具覆盖不了的复杂查询，如聚合统计、条件筛选、多属性对比等。
                                使用时请将用户问题作为 question 参数传入，尽量指定 deviceId 缩小查询范围。""")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("question", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("用户的自然语言查询问题")
                                        .build())
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("设备ID（可选），缩小查询范围")
                                        .build())
                                .property("productKey", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("产品类型（可选），例如 TEMP_SENSOR_V1")
                                        .build())
                                .required(List.of("question"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = JsonUtil.readTree(argumentsJson);
            String question = args.get("question").asText();
            String deviceId = args.has("deviceId") && !args.get("deviceId").isNull()
                    ? args.get("deviceId").asText() : null;
            String productKey = args.has("productKey") && !args.get("productKey").isNull()
                    ? args.get("productKey").asText() : null;

            // 1. AI 生成 SQL
            String rawResponse = deepSeekClient.chat(SYSTEM_PROMPT, buildUserPrompt(question, deviceId, productKey));
            if (rawResponse == null) {
                return FunctionHandler.errorJson("AI 服务暂时不可用");
            }

            // 2. 解析结果
            String json = rawResponse.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                if (json.endsWith("```")) json = json.substring(0, json.lastIndexOf("```"));
                json = json.trim();
            }

            Map<String, Object> result = JsonUtil.fromJson(json,
                    new TypeReference<Map<String, Object>>() {});

            String sql = getStr(result, "sql");
            String explanation = getStr(result, "explanation");

            // 3. 执行 SQL
            List<Map<String, Object>> queryResults = null;
            int resultCount = 0;
            if (sql != null && !sql.isBlank() && !"null".equalsIgnoreCase(sql)) {
                queryResults = executeQuery(sql);
                resultCount = queryResults != null ? queryResults.size() : 0;
            }

            return String.format(
                    "{\"sql\":\"%s\",\"explanation\":\"%s\",\"resultCount\":%d,\"results\":%s}",
                    esc(sql), esc(explanation), resultCount,
                    queryResults != null ? JsonUtil.toJson(queryResults) : "[]");

        } catch (Exception e) {
            log.warn("NL2SQL 执行失败", e);
            return FunctionHandler.errorJson("查询执行失败: " + e.getMessage());
        }
    }

    /** 安全执行 SELECT 查询 */
    private List<Map<String, Object>> executeQuery(String sql) {
        String cleaned = sql.trim().replaceAll(";\\s*$", "");
        String upper = cleaned.toUpperCase();

        // 只读校验：必须以 SELECT 开头
        if (!upper.startsWith("SELECT")) {
            log.warn("非 SELECT 语句，拒绝执行: {}", cleaned.substring(0, Math.min(50, cleaned.length())));
            return null;
        }
        // 阻断多语句（去掉尾分号后仍含分号 = 拼接了第二条语句）
        if (cleaned.contains(";")) {
            log.warn("检测到多语句，拒绝执行: {}", cleaned.substring(0, Math.min(80, cleaned.length())));
            return null;
        }
        // 阻断写/DDL 关键字（即便包在 CTE / 子查询里）
        if (containsForbiddenKeyword(upper)) {
            log.warn("检测到非只读关键字，拒绝执行: {}", cleaned.substring(0, Math.min(80, cleaned.length())));
            return null;
        }
        // 仅允许查询 device_data 表
        if (!upper.contains("DEVICE_DATA")) {
            log.warn("查询未涉及 device_data 表，拒绝执行: {}", cleaned.substring(0, Math.min(80, cleaned.length())));
            return null;
        }

        // 用正则判断是否已有 LIMIT 子句（避免列名/字符串里含 "LIMIT" 误判）
        String limitedSql = cleaned;
        if (!LIMIT_CLAUSE.matcher(upper).find()) {
            limitedSql = cleaned + " LIMIT " + MAX_RESULT_ROWS;
        }
        try {
            long start = System.currentTimeMillis();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(limitedSql);
            log.info("NL2SQL 执行完成: {}ms, {}行", System.currentTimeMillis() - start, results.size());
            if (results.size() > MAX_RESULT_ROWS) {
                return results.subList(0, MAX_RESULT_ROWS);
            }
            return results;
        } catch (Exception e) {
            log.error("SQL 执行失败: {}", e.getMessage());
            return null;
        }
    }

    /** 以单词边界匹配 LIMIT 子句，避免列名/字符串误判 */
    private static final java.util.regex.Pattern LIMIT_CLAUSE =
            java.util.regex.Pattern.compile("\\bLIMIT\\b");

    /** 写操作 / DDL 关键字黑名单（按单词边界匹配） */
    private static final java.util.regex.Pattern FORBIDDEN_KEYWORDS =
            java.util.regex.Pattern.compile(
                    "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|GRANT|REVOKE|"
                            + "MERGE|CALL|COPY|VACUUM|REINDEX|COMMENT|SET|RESET|EXECUTE|"
                            + "INTO|RETURNING)\\b");

    private boolean containsForbiddenKeyword(String upperSql) {
        return FORBIDDEN_KEYWORDS.matcher(upperSql).find();
    }

    private String buildUserPrompt(String question, String deviceId, String productKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("请将以下自然语言查询转换为 SQL：\n\n");
        sb.append("查询: ").append(question).append("\n");
        if (deviceId != null) sb.append("\n限定设备: ").append(deviceId).append("\n");
        if (productKey != null) sb.append("\n限定产品类型: ").append(productKey).append("\n");
        return sb.toString();
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String esc(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.devicemind.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据查询服务 — 安全执行 NL2SQL 生成的 SQL
 * <p>
 * 只允许 SELECT 查询，自动加 LIMIT 防止数据过量。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryService {

    /** 单次查询最大返回行数 */
    private static final int MAX_RESULT_ROWS = 200;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 安全执行 SELECT 查询
     *
     * @param sql 需要执行的 SQL
     * @return 查询结果（列名→值），若 SQL 非法或执行失败返回 null
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            log.warn("SQL 为空，跳过执行");
            return null;
        }

        // 1. 安全检查：只允许 SELECT
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            log.warn("非 SELECT 语句，拒绝执行: {}...", sql.substring(0, Math.min(50, sql.length())));
            return null;
        }

        // 2. 自动加 LIMIT（如果未指定）
        String limitedSql = sql;
        if (!trimmed.contains("LIMIT")) {
            // 去掉末尾分号再追加 LIMIT
            limitedSql = sql.trim().replaceAll(";\\s*$", "") + " LIMIT " + MAX_RESULT_ROWS;
        }

        // 3. 执行查询
        try {
            long start = System.currentTimeMillis();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(limitedSql);
            long elapsed = System.currentTimeMillis() - start;
            log.info("SQL 执行完成, 耗时={}ms, 返回={}行", elapsed, results.size());

            // 4. 如果数据库返回超过限制，截断
            if (results.size() > MAX_RESULT_ROWS) {
                log.warn("查询结果超过 {} 行，截断至 {}", MAX_RESULT_ROWS, MAX_RESULT_ROWS);
                return results.subList(0, MAX_RESULT_ROWS);
            }
            return results;

        } catch (Exception e) {
            log.error("SQL 执行失败: {}", e.getMessage());
            return null;
        }
    }
}

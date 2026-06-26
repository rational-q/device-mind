package com.devicemind.common.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.MDC;

/**
 * 链路追踪上下文。
 * <p>
 * 基于 SLF4J {@link MDC} 承载 traceId，贯穿一次请求 / 一条设备消息的处理过程，
 * 日志 pattern 通过 {@code %X{traceId}} 输出，便于后续按 traceId 聚合分析异常日志。
 * <p>
 * 跨服务调用时，traceId 通过 HTTP 头 {@link #TRACE_ID_HEADER} 透传。
 */
public final class TraceContext {

    /** MDC 中 traceId 的 key，对应 logback pattern 的 %X{traceId} */
    public static final String TRACE_ID = "traceId";

    /** 跨服务透传 traceId 的 HTTP 头名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** traceId 长度（8 位随机字符） */
    private static final int TRACE_ID_LENGTH = 8;

    private TraceContext() {
    }

    /**
     * 生成一个 8 位随机 traceId（大小写字母 + 数字）。
     */
    public static String generate() {
        return RandomStringUtils.randomAlphanumeric(TRACE_ID_LENGTH);
    }

    /**
     * 将指定 traceId 放入 MDC；为空则自动生成。
     *
     * @param traceId 已有 traceId（如来自上游请求头），可为 null
     * @return 实际写入 MDC 的 traceId
     */
    public static String set(String traceId) {
        String id = (traceId == null || traceId.isBlank()) ? generate() : traceId;
        MDC.put(TRACE_ID, id);
        return id;
    }

    /**
     * 生成新的 traceId 并放入 MDC。
     *
     * @return 写入 MDC 的 traceId
     */
    public static String set() {
        return set(null);
    }

    /**
     * 获取当前 MDC 中的 traceId，可能为 null。
     */
    public static String get() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 清除当前线程 MDC 中的 traceId。处理结束务必调用，避免线程复用导致串号。
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}

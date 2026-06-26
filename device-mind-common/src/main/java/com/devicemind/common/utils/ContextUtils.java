package com.devicemind.common.utils;

import java.util.Optional;

/**
 * 上下文工具类 — 线程级上下文存储
 * <p>
 * 提供当前用户 ID 的 ThreadLocal 存取，后续可扩展 traceId、租户ID 等。
 * 对接认证体系时，在 Filter / Interceptor 中调用 {@link #setCurrentUserId(Long)} 写入，
 * 业务层通过 {@link #getCurrentUserId()} 读取。
 * <p>
 * 当前为空壳实现，始终返回 {@link Optional#empty()}。
 */
public class ContextUtils {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private ContextUtils() {
    }

    /**
     * 设置当前用户 ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前用户 ID
     *
     * @return 当前用户 ID，未设置时返回 empty
     */
    public static Optional<Long> getCurrentUserId() {
        return Optional.ofNullable(USER_ID.get());
    }

    /**
     * 清除当前线程的上下文（防止内存泄漏）
     */
    public static void clear() {
        USER_ID.remove();
    }
}

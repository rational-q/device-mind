package com.devicemind.common.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 幂等守卫 — 基于 Caffeine 内存缓存的去重组件
 * <p>
 * 用于 Consumer 端的消息去重，防止重试/重投导致的重复处理。
 * <p>
 * 使用示例：
 * <pre>{@code
 *   String key = request.getDeviceId() + "_" + request.getTimestamp();
 *   if (!idempotentGuard.tryAcquire(key)) {
 *       log.warn("重复消息，跳过: {}", key);
 *       return;
 *   }
 *   // 正常处理...
 * }</pre>
 * <p>
 * 注意：这是内存级去重，服务重启后会丢失。对于关键业务（如指令下发），
 * 应同时在数据库层增加唯一约束作为最后防线。
 */
@Slf4j
@Component
public class IdempotentGuard {

    /**
     * 10 分钟窗口，最多 100,000 条
     */
    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .recordStats()
            .build();

    /**
     * 尝试获取幂等锁
     *
     * @param key 幂等键（如 deviceId_timestamp 或 idempotencyKey）
     * @return true = 首次处理，false = 重复消息
     */
    public boolean tryAcquire(String key) {
        Boolean existing = cache.asMap().putIfAbsent(key, Boolean.TRUE);
        if (existing != null) {
            log.debug("幂等拦截重复消息: key={}", key);
            return false;
        }
        return true;
    }

    /**
     * 手动释放幂等锁（一般不需要，等过期即可）
     */
    public void release(String key) {
        cache.invalidate(key);
    }

    /**
     * 获取缓存统计信息（Prometheus 暴露用）
     */
    public String stats() {
        return String.format("size=%d, hitRate=%.2f", cache.estimatedSize(), cache.stats().hitRate());
    }
}

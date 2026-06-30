package com.devicemind.common.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 简易雪花 ID 生成器（生产环境建议替换为分布式 ID 方案）
 */
public final class SnowflakeId {

    private SnowflakeId() {}

    public static long nextId() {
        return System.currentTimeMillis() << 12
                | (long) (ThreadLocalRandom.current().nextInt(4096));
    }
}

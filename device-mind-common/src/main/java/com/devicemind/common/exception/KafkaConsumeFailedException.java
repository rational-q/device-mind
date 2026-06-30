package com.devicemind.common.exception;

/**
 * Kafka 消费失败异常
 * <p>
 * 当 Consumer 处理消息发生不可恢复的错误时抛出此异常，
 * 由 Spring Kafka 的 CommonErrorHandler 捕获并触发重试/死信流程。
 * <p>
 * 与普通 RuntimeException 的区别：
 * - 明确语义：消费失败（而非代码 bug）
 * - 便于 ErrorHandler 区分可重试 vs 不可重试异常
 */
public class KafkaConsumeFailedException extends RuntimeException {

    public KafkaConsumeFailedException(String message) {
        super(message);
    }

    public KafkaConsumeFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

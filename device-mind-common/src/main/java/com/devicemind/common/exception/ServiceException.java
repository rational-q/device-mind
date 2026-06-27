package com.devicemind.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 统一业务层异常，携带错误码与错误信息，
 * 由 {@link com.devicemind.core.GlobalExceptionHandler} 全局捕获并返回给前端。
 */
@Getter
public class ServiceException extends RuntimeException {

    /** 业务错误码，默认 500 */
    private final int code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}

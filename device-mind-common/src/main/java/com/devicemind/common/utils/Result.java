package com.devicemind.common.utils;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
@Getter
@Setter
public class Result<T> {

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 默认失败状态码 */
    public static final int FAIL_CODE = 500;

    /** 状态码 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS_CODE, "success", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（默认错误码 500）
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(FAIL_CODE, message, null);
    }
}

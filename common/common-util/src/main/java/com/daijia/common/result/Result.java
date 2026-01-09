package com.daijia.common.result;

import lombok.Data;

/**
 * 全局统一返回结果类
 */
@Data
public class Result<T> {

    // 用包装类型是因为包装类型可以为null，JSON能正确处理
    // 返回码
    private Integer code;

    // 返回消息
    private String message;

    // 返回数据
    private T data;

    public Result() {}

    protected static<T> Result<T> build(T data) {
        Result<T> result = new Result<>();
        if(data != null) {
            result.setData(data);
        }
        return result;
    }

    public static<T> Result<T> build(T body, Integer code, String message) {
        Result<T> result = build(body);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static<T> Result<T> build(T body, ResultCodeEnum resultCodeEnum) {
        Result<T> result = build(body);
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

    public static<T> Result<T> ok() {
        return Result.ok();
    }

    public static<T> Result<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }

    public static<T> Result<T> fail() {
        return Result.fail();
    }

    public static<T> Result<T> fail(T data) {
        return build(data, ResultCodeEnum.FAIL);
    }

    public Result<T> message(String message) {
        this.message = message;
        return this;
    }

    public Result<T> code(Integer code) {
        this.setCode(code);
        return this;
    }
}

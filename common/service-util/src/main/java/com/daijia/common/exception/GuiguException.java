package com.daijia.common.exception;

import com.daijia.common.result.ResultCodeEnum;
import lombok.Data;

/**
 * 自定义全局异常类
 */
@Data
public class GuiguException extends RuntimeException {

    private Integer code;

    private String msg;

    /**
     * 通过状态码和错误消息创建异常对象
     * @param code
     * @param msg
     */
    public GuiguException(Integer code, String msg) {
        // Java规定：子类构造方法必须调用父类构造方法
        // 如果不写，编译器会自动加super()（无参构造）
        // 但RuntimeException没有无参构造传入message的方式
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    /**
     * 接收枚举类型对象
     * @param resultCodeEnum
     */
    public GuiguException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
        this.msg = resultCodeEnum.getMessage();
    }

    @Override
    public String toString() {
        return "GuiguException{" +
                "code=" + code +
                ", msg=" + this.getMsg() + "}";
    }
}

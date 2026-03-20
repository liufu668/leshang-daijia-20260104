package com.daijia.common.handler;

import com.daijia.common.exception.GuiguException;
import com.daijia.common.result.Result;
import com.daijia.common.result.ResultCodeEnum;
import feign.FeignException;
import feign.codec.DecodeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理类（工作标准版）
 * 核心：4大类异常全覆盖 + 日志规范 + 返回友好 + 无重复代码
 */
@Slf4j
@ControllerAdvice
@ResponseBody // 统一加在类上，不用每个方法都写
public class GlobalExceptionHandler {

    // ========== 1. 自定义业务异常（自己抛的，最常用） ==========
    @ExceptionHandler(GuiguException.class)
    public Result<?> handleGuiguException(GuiguException e) {
        // 只打业务异常msg，不用打堆栈（因为是预期内的）
        log.error("业务异常：{}", e.getMessage());
        return Result.build(null, e.getCode(), e.getMessage());
    }

    // ========== 2. 参数异常（前端传错，细分2类） ==========
    // 2.1 @Valid/@Validated 参数校验失败（BindException/MethodArgumentNotValidException）
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public Result<?> handleParamValidException(Exception e) {
        BindingResult result = null;
        if (e instanceof BindException) {
            result = ((BindException) e).getBindingResult();
        } else if (e instanceof MethodArgumentNotValidException) {
            result = ((MethodArgumentNotValidException) e).getBindingResult();
        }

        Map<String, String> errorMap = new HashMap<>();
        if (result != null) {
            List<FieldError> fieldErrors = result.getFieldErrors();
            fieldErrors.forEach(error -> {
                String field = error.getField(); // 错误字段名
                String msg = error.getDefaultMessage(); // 错误提示
                log.error("参数校验异常：字段{}，提示{}", field, msg);
                errorMap.put(field, msg);
            });
        }
        return Result.build(errorMap, ResultCodeEnum.ARGUMENT_VALID_ERROR.getCode(), "参数校验失败");
    }

    // 2.2 非法参数/类型不匹配（比如传字符串给数字字段）
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public Result<?> handleIllegalParamException(Exception e) {
        log.error("非法参数异常：", e); // 打堆栈，便于排查代码里的参数错误
        return Result.build(null, ResultCodeEnum.ARGUMENT_VALID_ERROR.getCode(), "参数格式/值错误：" + e.getMessage());
    }

    // ========== 3. 远程调用异常（Feign相关） ==========
    @ExceptionHandler({FeignException.class, DecodeException.class})
    public Result<?> handleFeignException(Exception e) {
        log.error("Feign远程调用异常：", e); // 必须打堆栈，定位哪个服务调用失败
        String msg = e instanceof FeignException ? "远程服务调用失败：" + ((FeignException) e).status() : "远程服务响应解析失败";
        return Result.build(null, ResultCodeEnum.REMOTE_CALL_ERROR.getCode(), msg);
    }

    // ========== 4. 数据层异常（数据库/Redis） ==========
    @ExceptionHandler(DataAccessException.class)
    public Result<?> handleDataException(DataAccessException e) {
        log.error("数据库/Redis异常：", e);
        return Result.build(null, ResultCodeEnum.DATA_ACCESS_ERROR.getCode(), "数据操作失败，请稍后重试");
    }

    // ========== 5. 兜底系统异常（所有未覆盖的异常，如NPE、超时等） ==========
    @ExceptionHandler(Exception.class)
    public Result<?> handleDefaultException(Exception e) {
        log.error("系统异常：", e); // 必须打完整堆栈，排查代码bug
        // 返回通用提示，不暴露具体错误
        return Result.build(null, ResultCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
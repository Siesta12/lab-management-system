package com.nlt.common.exception;

import com.nlt.common.api.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理异常处理结果
     * @param ex 参数
     * @return 响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理异常处理结果
     * @param ex 参数
     * @return 响应结果
     */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        BindException.class,
        HttpMessageNotReadableException.class
    })

    public ApiResponse<Void> handleBadRequest(Exception ex) {
        return ApiResponse.error(400, "请求参数错误");
    }

    /**
     * 处理异常处理结果
     * @param ex 参数
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.error(500, ex.getMessage() == null ? "服务器内部错误" : ex.getMessage());
    }

}

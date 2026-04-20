package com.nlt.common.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 构造业务异常
     * @param message 异常信息
     */
    public BusinessException(String message) {
        this(400, message);
    }

    /**
     * 构造业务异常
     * @param code 错误码
     * @param message 异常信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

}


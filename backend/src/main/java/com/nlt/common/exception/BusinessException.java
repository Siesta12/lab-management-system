package com.nlt.common.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 处理业务异常
     * @param message 参数
     * @return 处理结果
     */
    public BusinessException(String message) {
        this(400, message);
    }

    /**
     * 处理业务异常
     * @param code 参数
     * @param message 参数
     * @return 处理结果
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 查询业务异常
     * @return 处理结果
     */
    public int getCode() {
        return code;
    }

}

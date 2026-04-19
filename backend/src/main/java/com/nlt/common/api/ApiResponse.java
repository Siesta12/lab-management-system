package com.nlt.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 处理Api相关数据
     * @param data 参数
     * @return 响应结果
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "成功", data);
    }

    /**
     * 处理Api相关数据
     * @return 响应结果
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "成功", null);
    }

    /**
     * 处理Api相关数据
     * @param code 参数
     * @param message 参数
     * @return 响应结果
     */
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

}

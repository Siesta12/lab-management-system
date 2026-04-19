package com.nlt.service;

import com.nlt.domain.dto.auth.LoginRequest;
import com.nlt.domain.vo.auth.CurrentUserData;
import com.nlt.domain.vo.auth.LoginResponseData;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    /**
     * 用户登录
     * @param request 请求参数
     * @return 处理结果
     */
    LoginResponseData login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     * @param request HTTP请求
     * @return 处理结果
     */
    CurrentUserData currentUser(HttpServletRequest request);

    /**
     * 用户退出登录
     * @param request HTTP请求
     */
    void logout(HttpServletRequest request);

}

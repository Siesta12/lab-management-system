package com.nlt.service;

import com.nlt.domain.dto.auth.LoginRequest;
import com.nlt.domain.vo.auth.CurrentUserData;
import com.nlt.domain.vo.auth.LoginResponseData;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponseData login(LoginRequest request);

    CurrentUserData currentUser(HttpServletRequest request);

    void logout(HttpServletRequest request);

}


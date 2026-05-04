package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.domain.dto.auth.LoginRequest;
import com.nlt.domain.vo.auth.CurrentUserData;
import com.nlt.domain.vo.auth.LoginResponseData;
import com.nlt.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponseData> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserData> me(HttpServletRequest request) {
        return ApiResponse.success(authService.currentUser(request));
    }

}


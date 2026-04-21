package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.auth.LoginRequest;
import com.nlt.domain.entity.UserEntity;
import com.nlt.domain.vo.auth.CurrentUserData;
import com.nlt.domain.vo.auth.LoginResponseData;
import jakarta.servlet.http.HttpServletRequest;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.AuthService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final TokenService tokenService;

    /**
     * 用户登录
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LoginResponseData login(LoginRequest request) {
        UserEntity user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "用户已被禁用");
        }
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(user.getId());
        userMapper.updateLastLoginAt(user.getId());
        LoginResponseData data = new LoginResponseData();
        data.setId(user.getId());
        data.setUsername(user.getUsername());
        data.setRealName(user.getRealName());
        data.setToken(tokenService.generateToken(user.getId(), user.getUsername(), roleCodes));
        data.setRoleCodes(roleCodes);
        data.setDepartmentId(user.getDepartmentId());
        return data;
    }

    /**
     * 获取当前登录用户信息
     * @param request HTTP请求
     * @return 处理结果
     */
    @Override
    public CurrentUserData currentUser(HttpServletRequest request) {
        Long userId = tokenService.getCurrentUserId(request);
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        CurrentUserData data = new CurrentUserData();
        data.setId(user.getId());
        data.setUsername(user.getUsername());
        data.setRealName(user.getRealName());
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
        data.setRoleCodes(roleCodes);
        data.setDepartmentId(user.getDepartmentId());
        return data;
    }

    /**
     * 用户退出登录
     * @param request HTTP请求
     */
    @Override
    public void logout(HttpServletRequest request) {
        String token = tokenService.resolveToken(request);
        tokenService.parseClaims(token);
    }

}


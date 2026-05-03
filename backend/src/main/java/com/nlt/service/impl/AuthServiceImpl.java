package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.auth.LoginRequest;
import com.nlt.domain.entity.UserEntity;
import com.nlt.domain.vo.auth.CurrentUserData;
import com.nlt.domain.vo.auth.LoginResponseData;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final TokenService tokenService;

    @Override
    public LoginResponseData login(LoginRequest request) {
        String userNo = request.getUserNo() == null ? null : request.getUserNo().trim();
        if (userNo == null || userNo.isEmpty()) {
            throw new BusinessException(400, "学号/工号不能为空");
        }

        UserEntity user = userMapper.selectCredentialByUserNo(userNo);
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(401, "学号/工号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "用户已被禁用");
        }

        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(user.getId());
        userMapper.updateLastLoginAt(user.getId());

        LoginResponseData data = new LoginResponseData();
        data.setId(user.getId());
        data.setUserNo(user.getUserNo());
        data.setRealName(user.getRealName());
        data.setToken(tokenService.generateToken(user.getId(), user.getUserNo(), roleCodes));
        data.setRoleCodes(roleCodes);
        data.setDepartmentId(user.getDepartmentId());
        return data;
    }

    @Override
    public CurrentUserData currentUser(HttpServletRequest request) {
        Long userId = tokenService.getCurrentUserId(request);
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        CurrentUserData data = new CurrentUserData();
        data.setId(user.getId());
        data.setUserNo(user.getUserNo());
        data.setRealName(user.getRealName());
        data.setRoleCodes(userRoleMapper.selectRoleCodesByUserId(userId));
        data.setDepartmentId(user.getDepartmentId());
        return data;
    }

    @Override
    public void logout(HttpServletRequest request) {
        tokenService.parseClaims(tokenService.resolveToken(request));
    }
}

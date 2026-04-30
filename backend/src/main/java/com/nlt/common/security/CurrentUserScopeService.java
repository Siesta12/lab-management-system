package com.nlt.common.security;

import com.nlt.common.exception.BusinessException;
import com.nlt.domain.entity.UserEntity;
import com.nlt.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class CurrentUserScopeService {

    private final TokenService tokenService;
    private final UserMapper userMapper;

    public Long currentUserIdOrNull() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        try {
            return tokenService.getCurrentUserId(request);
        } catch (BusinessException ex) {
            return null;
        }
    }

    public List<String> currentRoleCodes() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return List.of();
        }
        try {
            List<String> roleCodes = tokenService.getCurrentRoleCodes(request);
            return roleCodes == null ? Collections.emptyList() : roleCodes;
        } catch (BusinessException ex) {
            return List.of();
        }
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isTeacher() {
        return hasRole("TEACHER");
    }

    public boolean isStudent() {
        return hasRole("STUDENT");
    }

    public boolean hasRole(String roleCode) {
        return currentRoleCodes().stream().anyMatch(code ->
            roleCode.equalsIgnoreCase(code) || ("ROLE_" + roleCode).equalsIgnoreCase(code)
        );
    }

    public Long requireCurrentDepartmentId() {
        UserEntity user = currentUser();
        if (user == null || user.getDepartmentId() == null) {
            throw new BusinessException(403, "当前账号未绑定所属学院");
        }
        return user.getDepartmentId();
    }

    public Long resolveAdminDepartmentId() {
        if (!isAdmin()) {
            return null;
        }
        UserEntity user = currentUser();
        if (user == null || user.getDepartmentId() == null) {
            throw new BusinessException(403, "当前学院管理员未绑定所属学院");
        }
        return user.getDepartmentId();
    }

    public Long resolveDepartmentFilter(Long requestedDepartmentId) {
        Long adminDepartmentId = resolveAdminDepartmentId();
        if (adminDepartmentId != null) {
            return adminDepartmentId;
        }
        UserEntity user = currentUser();
        if (user != null && user.getDepartmentId() != null) {
            return user.getDepartmentId();
        }
        return requestedDepartmentId;
    }

    public void ensureDepartmentAccessible(Long departmentId, String notFoundMessage) {
        Long adminDepartmentId = resolveAdminDepartmentId();
        if (adminDepartmentId != null && !Objects.equals(adminDepartmentId, departmentId)) {
            throw new BusinessException(404, notFoundMessage);
        }
    }

    public void ensureCurrentDepartmentAccessible(Long departmentId, String notFoundMessage) {
        Long currentDepartmentId = requireCurrentDepartmentId();
        if (!Objects.equals(currentDepartmentId, departmentId)) {
            throw new BusinessException(404, notFoundMessage);
        }
    }

    private UserEntity currentUser() {
        Long userId = currentUserIdOrNull();
        return userId == null ? null : userMapper.selectById(userId);
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }
}

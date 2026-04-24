package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.common.StatusUpdateRequest;
import com.nlt.domain.dto.user.PasswordResetRequest;
import com.nlt.domain.dto.user.PasswordUpdateRequest;
import com.nlt.domain.dto.user.UserCreateRequest;
import com.nlt.domain.dto.user.UserProfileUpdateRequest;
import com.nlt.domain.dto.user.UserUpdateRequest;
import com.nlt.domain.entity.ViolationRecordEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.domain.vo.user.UserVO;
import com.nlt.service.UserService;
import com.nlt.service.ViolationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ViolationService violationService;
    private final TokenService tokenService;

    @GetMapping
    public ApiResponse<PageData<UserVO>> page(HttpServletRequest request,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(name = "userNo", required = false) String userNo,
        @RequestParam(required = false) String realName,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) String roleCode,
        @RequestParam(required = false) Integer status) {
        Long scopedDepartmentId = resolveAdminDepartmentId(request);
        return ApiResponse.success(userService.page(pageNum, pageSize, userNo, realName, scopedDepartmentId, roleCode, status));
    }

    @PostMapping
    public ApiResponse<UserVO> create(HttpServletRequest request, @Valid @RequestBody UserCreateRequest body) {
        Long scopedDepartmentId = resolveAdminDepartmentId(request);
        body.setDepartmentId(scopedDepartmentId);
        return ApiResponse.created(userService.create(body));
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.options(status));
    }

    @GetMapping("/profile")
    public ApiResponse<UserVO> profile(HttpServletRequest request) {
        return ApiResponse.success(userService.currentUser(tokenService.getCurrentUserId(request)));
    }

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(HttpServletRequest request, @RequestBody UserProfileUpdateRequest req) {
        return ApiResponse.success(userService.updateProfile(tokenService.getCurrentUserId(request), req));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request, @Valid @RequestBody PasswordUpdateRequest req) {
        userService.updatePassword(tokenService.getCurrentUserId(request), req);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(HttpServletRequest request, @PathVariable Long id) {
        UserVO user = requireSameDepartment(request, id);
        return ApiResponse.success(user);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(HttpServletRequest request, @PathVariable Long id,
        @RequestBody UserUpdateRequest body) {
        Long scopedDepartmentId = resolveAdminDepartmentId(request);
        requireSameDepartment(request, id);
        body.setDepartmentId(scopedDepartmentId);
        return ApiResponse.success(userService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        requireSameDepartment(request, id);
        userService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(HttpServletRequest request, @PathVariable Long id,
        @Valid @RequestBody PasswordResetRequest body) {
        requireSameDepartment(request, id);
        userService.resetPassword(id, body);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(HttpServletRequest request, @PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest body) {
        requireSameDepartment(request, id);
        userService.updateStatus(id, body.getStatus());
        return ApiResponse.success();
    }

    @GetMapping("/{id}/violations")
    public ApiResponse<PageData<ViolationRecordEntity>> violations(HttpServletRequest request, @PathVariable Long id,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        requireSameDepartment(request, id);
        return ApiResponse.success(violationService.page(pageNum, pageSize, id, null, null));
    }

    private Long resolveAdminDepartmentId(HttpServletRequest request) {
        var roleCodes = tokenService.getCurrentRoleCodes(request);
        boolean admin = roleCodes != null && roleCodes.stream()
            .anyMatch(code -> code != null && code.toUpperCase().contains("ADMIN"));
        if (!admin) {
            throw new BusinessException(403, "无权访问");
        }

        Long currentUserId = tokenService.getCurrentUserId(request);
        UserVO currentUser = userService.currentUser(currentUserId);
        if (currentUser.getDepartmentId() == null) {
            throw new BusinessException(403, "当前账号未绑定学院");
        }
        return currentUser.getDepartmentId();
    }

    private UserVO requireSameDepartment(HttpServletRequest request, Long targetUserId) {
        Long scopedDepartmentId = resolveAdminDepartmentId(request);
        UserVO user = userService.getById(targetUserId);
        if (user.getDepartmentId() == null || !scopedDepartmentId.equals(user.getDepartmentId())) {
            throw new BusinessException(403, "无权访问其他学院的用户");
        }
        return user;
    }
}

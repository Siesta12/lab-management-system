package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.common.StatusUpdateRequest;
import com.nlt.domain.dto.user.*;
import com.nlt.domain.entity.ViolationRecordEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.domain.vo.user.UserVO;
import com.nlt.service.UserService;
import com.nlt.service.ViolationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ViolationService violationService;
    private final TokenService tokenService;

    /**
     * 分页查询用户列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param username 用户名
     * @param realName 真实姓名
     * @param departmentId 部门ID
     * @param status 状态
     * @return 分页数据
     */
    @GetMapping
    public ApiResponse<PageData<UserVO>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String realName,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) String roleCode,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.page(pageNum, pageSize, username, realName, departmentId, roleCode, status));
    }

    /**
     * 创建用户
     * @param request 创建请求
     * @return 创建后的用户信息
     */
    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.created(userService.create(request));
    }

    /**
     * 获取用户选项列表
     * @param status 状态
     * @return 选项列表
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.options(status));
    }

    /**
     * 获取当前用户信息
     * @param request HTTP请求
     * @return 当前用户信息
     */
    @GetMapping("/profile")
    public ApiResponse<UserVO> profile(HttpServletRequest request) {
        return ApiResponse.success(userService.currentUser(tokenService.getCurrentUserId(request)));
    }

    /**
     * 更新当前用户资料
     * @param request HTTP请求
     * @param req 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(HttpServletRequest request,
        @RequestBody UserProfileUpdateRequest req) {
        return ApiResponse.success(userService.updateProfile(tokenService.getCurrentUserId(request), req));
    }

    /**
     * 更新当前用户密码
     * @param request HTTP请求
     * @param req 密码更新请求
     * @return 操作结果
     */
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request,
        @Valid @RequestBody PasswordUpdateRequest req) {
        userService.updatePassword(tokenService.getCurrentUserId(request), req);
        return ApiResponse.success();
    }

    /**
     * 根据ID获取用户详情
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /**
     * 更新用户信息
     * @param id 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id,
        @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    /**
     * 删除用户
     * @param id 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 重置用户密码
     * @param id 用户ID
     * @param request 密码重置请求
     * @return 操作结果
     */
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
        @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }

    /**
     * 更新用户状态
     * @param id 用户ID
     * @param request 状态更新请求
     * @return 操作结果
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        userService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 获取用户违规记录列表
     * @param id 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 违规记录分页数据
     */
    @GetMapping("/{id}/violations")
    public ApiResponse<PageData<ViolationRecordEntity>> violations(@PathVariable Long id,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(violationService.page(pageNum, pageSize, id, null, null));
    }

}


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
     * 查询用户信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param username 参数
     * @param realName 参数
     * @param departmentId 部门ID
     * @param status 状态值
     * @return 响应结果
     */
    @GetMapping
    public ApiResponse<PageData<UserVO>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String realName,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.page(pageNum, pageSize, username, realName, departmentId, status));
    }

    /**
     * 新增用户信息
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    /**
     * 处理用户信息
     * @param status 状态值
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.options(status));
    }

    /**
     * 处理用户信息
     * @param request 请求参数
     * @return 响应结果
     */
    @GetMapping("/profile")
    public ApiResponse<UserVO> profile(HttpServletRequest request) {
        return ApiResponse.success(userService.currentUser(tokenService.getCurrentUserId(request)));
    }

    /**
     * 更新用户信息
     * @param request 请求参数
     * @param req 参数
     * @return 响应结果
     */
    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(HttpServletRequest request,
        @RequestBody UserProfileUpdateRequest req) {
        return ApiResponse.success(userService.updateProfile(tokenService.getCurrentUserId(request), req));
    }

    /**
     * 更新用户信息
     * @param request 请求参数
     * @param req 参数
     * @return 响应结果
     */
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request,
        @Valid @RequestBody PasswordUpdateRequest req) {
        userService.updatePassword(tokenService.getCurrentUserId(request), req);
        return ApiResponse.success();
    }

    /**
     * 查询用户信息
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /**
     * 更新用户信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id,
        @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    /**
     * 删除用户信息
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 重置用户信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
        @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }

    /**
     * 更新用户信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        userService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 处理用户信息
     * @param id 主键ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 响应结果
     */
    @GetMapping("/{id}/violations")
    public ApiResponse<PageData<ViolationRecordEntity>> violations(@PathVariable Long id,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(violationService.page(pageNum, pageSize, id, null, null));
    }

}

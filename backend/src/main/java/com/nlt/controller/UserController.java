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
     * 鏌ヨ鐢ㄦ埛淇℃伅鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param username 鍙傛暟
     * @param realName 鍙傛暟
     * @param departmentId 閮ㄩ棬ID
     * @param status 鐘舵€佸€?
     * @return 鍝嶅簲缁撴灉
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
     * 鏂板鐢ㄦ埛淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.created(userService.create(request));
    }

    /**
     * 澶勭悊鐢ㄦ埛淇℃伅
     * @param status 鐘舵€佸€?
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.options(status));
    }

    /**
     * 澶勭悊鐢ㄦ埛淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/profile")
    public ApiResponse<UserVO> profile(HttpServletRequest request) {
        return ApiResponse.success(userService.currentUser(tokenService.getCurrentUserId(request)));
    }

    /**
     * 鏇存柊鐢ㄦ埛淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @param req 鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(HttpServletRequest request,
        @RequestBody UserProfileUpdateRequest req) {
        return ApiResponse.success(userService.updateProfile(tokenService.getCurrentUserId(request), req));
    }

    /**
     * 鏇存柊鐢ㄦ埛淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @param req 鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request,
        @Valid @RequestBody PasswordUpdateRequest req) {
        userService.updatePassword(tokenService.getCurrentUserId(request), req);
        return ApiResponse.success();
    }

    /**
     * 鏌ヨ鐢ㄦ埛淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /**
     * 鏇存柊鐢ㄦ埛淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id,
        @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    /**
     * 鍒犻櫎鐢ㄦ埛淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 閲嶇疆鐢ㄦ埛淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
        @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }

    /**
     * 鏇存柊鐢ㄦ埛淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        userService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 澶勭悊鐢ㄦ埛淇℃伅
     * @param id 涓婚敭ID
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}/violations")
    public ApiResponse<PageData<ViolationRecordEntity>> violations(@PathVariable Long id,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(violationService.page(pageNum, pageSize, id, null, null));
    }

}

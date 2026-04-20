package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.violation.ViolationSaveRequest;
import com.nlt.domain.entity.ViolationRecordEntity;
import com.nlt.service.ViolationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;
    private final TokenService tokenService;

    /**
     * 鏌ヨ杩濊璁板綍鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param userId 鐢ㄦ埛ID
     * @param reservationId 棰勭害ID
     * @param violationType 鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping
    public ApiResponse<PageData<ViolationRecordEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long reservationId,
        @RequestParam(required = false) Integer violationType) {
        return ApiResponse.success(violationService.page(pageNum, pageSize, userId, reservationId, violationType));
    }

    /**
     * 鏂板杩濊璁板綍
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<ViolationRecordEntity> create(@Valid @RequestBody ViolationSaveRequest request) {
        return ApiResponse.created(violationService.create(request));
    }

    /**
     * 鏌ヨ褰撳墠鐢ㄦ埛棰勭害淇℃伅鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param servletRequest HTTP璇锋眰瀵硅薄
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/mine")
    public ApiResponse<PageData<ViolationRecordEntity>> mine(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(violationService.mine(tokenService.getCurrentUserId(servletRequest), pageNum, pageSize));
    }

    /**
     * 鏌ヨ杩濊璁板綍
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<ViolationRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(violationService.getById(id));
    }

    /**
     * 鍒犻櫎杩濊璁板綍
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        violationService.delete(id);
        return ApiResponse.success();
    }

}

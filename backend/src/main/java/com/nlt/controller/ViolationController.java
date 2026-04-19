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
     * 查询违规记录列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @param violationType 参数
     * @return 响应结果
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
     * 新增违规记录
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<ViolationRecordEntity> create(@Valid @RequestBody ViolationSaveRequest request) {
        return ApiResponse.success(violationService.create(request));
    }

    /**
     * 查询当前用户预约信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @GetMapping("/mine")
    public ApiResponse<PageData<ViolationRecordEntity>> mine(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(violationService.mine(tokenService.getCurrentUserId(servletRequest), pageNum, pageSize));
    }

    /**
     * 查询违规记录
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<ViolationRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(violationService.getById(id));
    }

    /**
     * 删除违规记录
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        violationService.delete(id);
        return ApiResponse.success();
    }

}

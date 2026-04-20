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
     * 分页查询违规记录
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @param violationType 违规类型
     * @return 违规记录分页数据
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
     * 创建违规记录
     * @param request 请求参数
     * @return 创建的违规记录
     */
    @PostMapping
    public ApiResponse<ViolationRecordEntity> create(@Valid @RequestBody ViolationSaveRequest request) {
        return ApiResponse.created(violationService.create(request));
    }

    /**
     * 分页查询当前用户的违规记录
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param servletRequest HTTP请求对象
     * @return 违规记录分页数据
     */
    @GetMapping("/mine")
    public ApiResponse<PageData<ViolationRecordEntity>> mine(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(violationService.mine(tokenService.getCurrentUserId(servletRequest), pageNum, pageSize));
    }

    /**
     * 根据ID查询违规记录
     * @param id 违规记录ID
     * @return 违规记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ViolationRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(violationService.getById(id));
    }

    /**
     * 删除违规记录
     * @param id 违规记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        violationService.delete(id);
        return ApiResponse.success();
    }

}


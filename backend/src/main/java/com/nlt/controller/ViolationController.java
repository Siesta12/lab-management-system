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

    @GetMapping
    public ApiResponse<PageData<ViolationRecordEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long reservationId,
        @RequestParam(required = false) Integer violationType) {
        return ApiResponse.success(violationService.page(pageNum, pageSize, userId, reservationId, violationType));
    }

    @PostMapping
    public ApiResponse<ViolationRecordEntity> create(@Valid @RequestBody ViolationSaveRequest request) {
        return ApiResponse.created(violationService.create(request));
    }

    @GetMapping("/mine")
    public ApiResponse<PageData<ViolationRecordEntity>> mine(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Integer violationType,
        @RequestParam(required = false) Integer scoreDirection,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(
            violationService.mine(tokenService.getCurrentUserId(servletRequest), pageNum, pageSize, violationType, scoreDirection)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ViolationRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(violationService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        violationService.delete(id);
        return ApiResponse.success();
    }

}

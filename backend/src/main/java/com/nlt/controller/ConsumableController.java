package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.entity.ExperimentReportConsumableEntity;
import com.nlt.service.ConsumableService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumables")
@RequiredArgsConstructor
public class ConsumableController {

    private final ConsumableService consumableService;
    private final TokenService tokenService;

    @GetMapping
    public ApiResponse<PageData<ConsumableEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String labType,
        @RequestParam(required = false) String consumableName,
        @RequestParam(required = false) String consumableCode,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Boolean warningOnly) {
        return ApiResponse.success(consumableService.page(pageNum, pageSize, labId, labType, consumableName, consumableCode, status, warningOnly));
    }

    @GetMapping("/options")
    public ApiResponse<List<ConsumableEntity>> options(@RequestParam Long labId) {
        return ApiResponse.success(consumableService.availableOptions(labId));
    }

    @GetMapping("/usages")
    public ApiResponse<PageData<ExperimentReportConsumableEntity>> usagePage(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String keyword) {
        return ApiResponse.success(consumableService.usagePage(pageNum, pageSize, status, labId, keyword));
    }

    @PostMapping
    public ApiResponse<ConsumableEntity> create(@Valid @RequestBody ConsumableSaveRequest request) {
        return ApiResponse.created(consumableService.create(request));
    }

    @GetMapping("/low-stock")
    public ApiResponse<PageData<ConsumableEntity>> lowStock(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(consumableService.warningList(pageNum, pageSize));
    }

    @GetMapping("/warning-list")
    public ApiResponse<PageData<ConsumableEntity>> warningList(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(consumableService.warningList(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConsumableEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(consumableService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConsumableEntity> update(@PathVariable Long id,
        @Valid @RequestBody ConsumableSaveRequest request) {
        return ApiResponse.success(consumableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        consumableService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/stock")
    public ApiResponse<ConsumableEntity> updateStock(@PathVariable Long id,
        @Valid @RequestBody ConsumableStockUpdateRequest request,
        HttpServletRequest servletRequest) {
        Long userId = tokenService.getCurrentUserId(servletRequest);
        return ApiResponse.success(consumableService.updateStock(id, request, userId));
    }

}


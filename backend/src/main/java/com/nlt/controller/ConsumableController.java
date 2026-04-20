package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.service.ConsumableService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumables")
@RequiredArgsConstructor
public class ConsumableController {

    private final ConsumableService consumableService;
    private final TokenService tokenService;

    /**
     * 分页查询耗材列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param labId 实验室ID
     * @param consumableName 耗材名称
     * @param consumableCode 耗材编码
     * @return 分页数据
     */
    @GetMapping
    public ApiResponse<PageData<ConsumableEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String consumableName,
        @RequestParam(required = false) String consumableCode) {
        return ApiResponse.success(consumableService.page(pageNum, pageSize, labId, consumableName, consumableCode));
    }

    /**
     * 创建耗材
     * @param request 创建请求参数
     * @return 创建的耗材信息
     */
    @PostMapping
    public ApiResponse<ConsumableEntity> create(@Valid @RequestBody ConsumableSaveRequest request) {
        return ApiResponse.created(consumableService.create(request));
    }

    /**
     * 获取低库存耗材列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页数据
     */
    @GetMapping("/low-stock")
    public ApiResponse<PageData<ConsumableEntity>> lowStock(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(consumableService.warningList(pageNum, pageSize));
    }

    /**
     * 获取库存预警列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页数据
     */
    @GetMapping("/warning-list")
    public ApiResponse<PageData<ConsumableEntity>> warningList(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(consumableService.warningList(pageNum, pageSize));
    }

    /**
     * 根据ID查询耗材详情
     * @param id 耗材ID
     * @return 耗材信息
     */
    @GetMapping("/{id}")
    public ApiResponse<ConsumableEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(consumableService.getById(id));
    }

    /**
     * 更新耗材信息
     * @param id 耗材ID
     * @param request 更新请求参数
     * @return 更新后的耗材信息
     */
    @PutMapping("/{id}")
    public ApiResponse<ConsumableEntity> update(@PathVariable Long id,
        @RequestBody ConsumableSaveRequest request) {
        return ApiResponse.success(consumableService.update(id, request));
    }

    /**
     * 删除耗材
     * @param id 耗材ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        consumableService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新耗材库存
     * @param id 耗材ID
     * @param request 库存更新请求参数
     * @param servletRequest HTTP请求对象
     * @return 更新后的耗材信息
     */
    @PatchMapping("/{id}/stock")
    public ApiResponse<ConsumableEntity> updateStock(@PathVariable Long id,
        @Valid @RequestBody ConsumableStockUpdateRequest request,
        HttpServletRequest servletRequest) {
        Long userId = tokenService.getCurrentUserId(servletRequest);
        return ApiResponse.success(consumableService.updateStock(id, request, userId));
    }

}


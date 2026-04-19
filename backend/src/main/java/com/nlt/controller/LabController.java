package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.common.StatusUpdateRequest;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.service.LabService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    /**
     * 查询实验室信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labName 参数
     * @param labCode 参数
     * @param labType 参数
     * @param departmentId 部门ID
     * @param openStatus 参数
     * @param labStatus 参数
     * @return 响应结果
     */
    @GetMapping
    public ApiResponse<PageData<LabEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String labName,
        @RequestParam(required = false) String labCode,
        @RequestParam(required = false) String labType,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Integer openStatus,
        @RequestParam(required = false) Integer labStatus) {
        return ApiResponse.success(labService.page(pageNum, pageSize, labName, labCode, labType, departmentId, openStatus, labStatus));
    }

    /**
     * 新增实验室信息
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<LabEntity> create(@Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.success(labService.create(request));
    }

    /**
     * 处理实验室信息
     * @param openStatus 参数
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer openStatus) {
        return ApiResponse.success(labService.options(openStatus));
    }

    /**
     * 查询实验室信息
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<LabEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(labService.getById(id));
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public ApiResponse<LabEntity> update(@PathVariable Long id,
        @Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.success(labService.update(id, request));
    }

    /**
     * 删除实验室信息
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PatchMapping("/{id}/open-status")
    public ApiResponse<Void> updateOpenStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        labService.updateOpenStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PatchMapping("/{id}/lab-status")
    public ApiResponse<Void> updateLabStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        labService.updateLabStatus(id, request.getStatus());
        return ApiResponse.success();
    }

}

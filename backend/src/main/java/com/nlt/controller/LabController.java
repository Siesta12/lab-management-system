package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.common.StatusUpdateRequest;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.dto.lab.LabMaintenanceCreateRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.domain.vo.schedule.DailyScheduleResponse;
import com.nlt.domain.vo.schedule.LabMaintenanceItem;
import com.nlt.domain.vo.schedule.LabScheduleResponse;
import com.nlt.service.LabService;
import com.nlt.service.LabScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;
    private final TokenService tokenService;
    private final LabScheduleService labScheduleService;

    /**
     * 分页查询实验室列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param labName 实验室名称
     * @param labCode 实验室编号
     * @param labType 实验室类型
     * @param departmentId 部门ID
     * @param openStatus 开放状态
     * @param labStatus 实验室状态
     * @return 分页数据
     */
    @GetMapping
    public ApiResponse<PageData<LabEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String labName,
        @RequestParam(required = false) String labCode,
        @RequestParam(required = false) String labType,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Integer openStatus,
        @RequestParam(required = false) Integer labStatus,
        HttpServletRequest request) {
        Long currentUserId = tokenService.getCurrentUserId(request);
        List<String> currentRoleCodes = tokenService.getCurrentRoleCodes(request);
        return ApiResponse.success(labService.page(pageNum, pageSize, labName, labCode, labType, departmentId,
            openStatus, labStatus, currentUserId, currentRoleCodes));
    }

    /**
     * 创建实验室
     * @param request 创建请求
     * @return 创建的实验室实体
     */
    @PostMapping
    public ApiResponse<LabEntity> create(@Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.created(labService.create(request));
    }

    /**
     * 获取实验室选项列表
     * @param openStatus 开放状态
     * @return 选项列表
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer openStatus,
        HttpServletRequest request) {
        Long currentUserId = tokenService.getCurrentUserId(request);
        List<String> currentRoleCodes = tokenService.getCurrentRoleCodes(request);
        return ApiResponse.success(labService.options(openStatus, currentUserId, currentRoleCodes));
    }

    /**
     * 根据ID查询实验室详情
     * @param id 实验室ID
     * @return 实验室实体
     */
    @GetMapping("/{id}")
    public ApiResponse<LabEntity> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = tokenService.getCurrentUserId(request);
        List<String> currentRoleCodes = tokenService.getCurrentRoleCodes(request);
        return ApiResponse.success(labService.getById(id, currentUserId, currentRoleCodes));
    }

    /**
     * 更新实验室信息
     * @param id 实验室ID
     * @param request 更新请求
     * @return 更新后的实验室实体
     */
    @PutMapping("/{id}")
    public ApiResponse<LabEntity> update(@PathVariable Long id,
        @Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.success(labService.update(id, request));
    }

    /**
     * 删除实验室
     * @param id 实验室ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新实验室开放状态
     * @param id 实验室ID
     * @param request 状态更新请求
     * @return 操作结果
     */
    @PatchMapping("/{id}/open-status")
    public ApiResponse<Void> updateOpenStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        labService.updateOpenStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 更新实验室状态
     * @param id 实验室ID
     * @param request 状态更新请求
     * @return 操作结果
     */
    @PatchMapping("/{id}/lab-status")
    public ApiResponse<Void> updateLabStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        labService.updateLabStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    @GetMapping("/{labId}/schedule")
    public ApiResponse<LabScheduleResponse> schedule(@PathVariable Long labId,
        @RequestParam(required = false) String startDate,
        HttpServletRequest request) {
        return ApiResponse.success(labScheduleService.getLabSchedule(labId, startDate,
            tokenService.getCurrentUserId(request), tokenService.getCurrentRoleCodes(request)));
    }

    @GetMapping("/schedule/daily")
    public ApiResponse<DailyScheduleResponse> dailySchedule(@RequestParam(required = false) String date,
        HttpServletRequest request) {
        return ApiResponse.success(labScheduleService.getDailySchedule(date,
            tokenService.getCurrentUserId(request), tokenService.getCurrentRoleCodes(request)));
    }

    @PostMapping("/{labId}/maintenance")
    public ApiResponse<List<LabMaintenanceItem>> createMaintenance(@PathVariable Long labId,
        @Valid @RequestBody LabMaintenanceCreateRequest body,
        HttpServletRequest request) {
        return ApiResponse.created(labScheduleService.createMaintenance(labId, body,
            tokenService.getCurrentUserId(request), tokenService.getCurrentRoleCodes(request)));
    }

    @PutMapping("/{labId}/maintenance/{maintenanceId}/cancel")
    public ApiResponse<Void> cancelMaintenance(@PathVariable Long labId,
        @PathVariable Long maintenanceId,
        HttpServletRequest request) {
        labScheduleService.cancelMaintenance(labId, maintenanceId,
            tokenService.getCurrentUserId(request), tokenService.getCurrentRoleCodes(request));
        return ApiResponse.success();
    }

    @GetMapping("/{labId}/maintenance")
    public ApiResponse<List<LabMaintenanceItem>> listMaintenance(@PathVariable Long labId,
        HttpServletRequest request) {
        return ApiResponse.success(labScheduleService.listLabMaintenance(labId,
            tokenService.getCurrentUserId(request), tokenService.getCurrentRoleCodes(request)));
    }

}


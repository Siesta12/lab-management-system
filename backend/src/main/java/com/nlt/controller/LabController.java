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

    @GetMapping
    public ApiResponse<PageData<LabEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String labName,
        @RequestParam(required = false) String labCode,
        @RequestParam(required = false) String labType,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Integer openStatus,
        @RequestParam(required = false) Integer labStatus,
        HttpServletRequest request) {
        return ApiResponse.success(labService.page(pageNum, pageSize, labId, labName, labCode, labType, departmentId,
            openStatus, labStatus));
    }

    @PostMapping
    public ApiResponse<LabEntity> create(@Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.created(labService.create(request));
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer openStatus,
        @RequestParam(required = false) Long departmentId,
        HttpServletRequest request) {
        return ApiResponse.success(labService.options(openStatus, departmentId));
    }

    @GetMapping("/{id}")
    public ApiResponse<LabEntity> getById(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(labService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<LabEntity> update(@PathVariable Long id,
        @Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.success(labService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/open-status")
    public ApiResponse<Void> updateOpenStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        labService.updateOpenStatus(id, request.getStatus());
        return ApiResponse.success();
    }

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

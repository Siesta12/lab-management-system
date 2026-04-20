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
     * 鏌ヨ瀹為獙瀹や俊鎭垪琛?
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param labName 鍙傛暟
     * @param labCode 鍙傛暟
     * @param labType 鍙傛暟
     * @param departmentId 閮ㄩ棬ID
     * @param openStatus 鍙傛暟
     * @param labStatus 鍙傛暟
     * @return 鍝嶅簲缁撴灉
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
     * 鏂板瀹為獙瀹や俊鎭?
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<LabEntity> create(@Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.created(labService.create(request));
    }

    /**
     * 澶勭悊瀹為獙瀹や俊鎭?
     * @param openStatus 鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Integer openStatus,
        HttpServletRequest request) {
        Long currentUserId = tokenService.getCurrentUserId(request);
        List<String> currentRoleCodes = tokenService.getCurrentRoleCodes(request);
        return ApiResponse.success(labService.options(openStatus, currentUserId, currentRoleCodes));
    }

    /**
     * 鏌ヨ瀹為獙瀹や俊鎭?
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<LabEntity> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = tokenService.getCurrentUserId(request);
        List<String> currentRoleCodes = tokenService.getCurrentRoleCodes(request);
        return ApiResponse.success(labService.getById(id, currentUserId, currentRoleCodes));
    }

    /**
     * 鏇存柊瀹為獙瀹や俊鎭?
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/{id}")
    public ApiResponse<LabEntity> update(@PathVariable Long id,
        @Valid @RequestBody LabSaveRequest request) {
        return ApiResponse.success(labService.update(id, request));
    }

    /**
     * 鍒犻櫎瀹為獙瀹や俊鎭?
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 鏇存柊瀹為獙瀹や俊鎭?
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PatchMapping("/{id}/open-status")
    public ApiResponse<Void> updateOpenStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        labService.updateOpenStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 鏇存柊瀹為獙瀹や俊鎭?
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
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

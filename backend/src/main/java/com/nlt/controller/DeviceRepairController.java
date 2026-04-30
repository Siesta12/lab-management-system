package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.device.DeviceRepairCreateRequest;
import com.nlt.domain.dto.device.DeviceRepairStatusUpdateRequest;
import com.nlt.domain.entity.DeviceRepairEntity;
import com.nlt.service.DeviceRepairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device-repairs")
@RequiredArgsConstructor
public class DeviceRepairController {

    private final DeviceRepairService deviceRepairService;

    @GetMapping
    public ApiResponse<PageData<DeviceRepairEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(deviceRepairService.page(pageNum, pageSize, labId, deviceId, status));
    }

    @PostMapping
    public ApiResponse<DeviceRepairEntity> create(@Valid @RequestBody DeviceRepairCreateRequest request) {
        return ApiResponse.created(deviceRepairService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceRepairEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceRepairService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<DeviceRepairEntity> updateStatus(@PathVariable Long id,
        @Valid @RequestBody DeviceRepairStatusUpdateRequest request) {
        return ApiResponse.success(deviceRepairService.updateStatus(id, request));
    }
}

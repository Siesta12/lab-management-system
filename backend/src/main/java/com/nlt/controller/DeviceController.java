package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.common.StatusUpdateRequest;
import com.nlt.domain.dto.device.DeviceCreateRequest;
import com.nlt.domain.dto.device.DeviceUpdateRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ApiResponse<PageData<DeviceEntity>> page(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String labType,
        @RequestParam(required = false) String deviceName,
        @RequestParam(required = false) String deviceCode,
        @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(deviceService.page(pageNum, pageSize, labId, labType, deviceName, deviceCode, status));
    }

    @PostMapping
    public ApiResponse<DeviceEntity> create(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.created(deviceService.create(request));
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Long labId) {
        return ApiResponse.success(deviceService.options(labId));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceEntity> update(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        deviceService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }
}

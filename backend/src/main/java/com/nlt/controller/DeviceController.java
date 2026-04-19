package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.common.StatusUpdateRequest;
import com.nlt.domain.dto.device.DeviceSaveRequest;
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

    /**
     * 查询设备信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param deviceName 参数
     * @param deviceCode 参数
     * @param status 状态值
     * @return 响应结果
     */
    @GetMapping
    public ApiResponse<PageData<DeviceEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String deviceName,
        @RequestParam(required = false) String deviceCode,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(deviceService.page(pageNum, pageSize, labId, deviceName, deviceCode, status));
    }

    /**
     * 新增设备信息
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<DeviceEntity> create(@Valid @RequestBody DeviceSaveRequest request) {
        return ApiResponse.success(deviceService.create(request));
    }

    /**
     * 处理设备信息
     * @param labId 实验室ID
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Long labId) {
        return ApiResponse.success(deviceService.options(labId));
    }

    /**
     * 查询设备信息
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<DeviceEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getById(id));
    }

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public ApiResponse<DeviceEntity> update(@PathVariable Long id,
        @Valid @RequestBody DeviceSaveRequest request) {
        return ApiResponse.success(deviceService.update(id, request));
    }

    /**
     * 删除设备信息
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        deviceService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

}

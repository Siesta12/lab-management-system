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
     * 鏌ヨ璁惧淇℃伅鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param labId 瀹為獙瀹D
     * @param deviceName 鍙傛暟
     * @param deviceCode 鍙傛暟
     * @param status 鐘舵€佸€?
     * @return 鍝嶅簲缁撴灉
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
     * 鏂板璁惧淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<DeviceEntity> create(@Valid @RequestBody DeviceSaveRequest request) {
        return ApiResponse.created(deviceService.create(request));
    }

    /**
     * 澶勭悊璁惧淇℃伅
     * @param labId 瀹為獙瀹D
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options(@RequestParam(required = false) Long labId) {
        return ApiResponse.success(deviceService.options(labId));
    }

    /**
     * 鏌ヨ璁惧淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<DeviceEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getById(id));
    }

    /**
     * 鏇存柊璁惧淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/{id}")
    public ApiResponse<DeviceEntity> update(@PathVariable Long id,
        @Valid @RequestBody DeviceSaveRequest request) {
        return ApiResponse.success(deviceService.update(id, request));
    }

    /**
     * 鍒犻櫎璁惧淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 鏇存柊璁惧淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request) {
        deviceService.updateStatus(id, request.getStatus());
        return ApiResponse.success();
    }

}

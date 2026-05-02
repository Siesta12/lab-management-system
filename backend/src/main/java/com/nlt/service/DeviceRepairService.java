package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.device.DeviceRepairCreateRequest;
import com.nlt.domain.dto.device.DeviceRepairStatusUpdateRequest;
import com.nlt.domain.entity.DeviceRepairEntity;

public interface DeviceRepairService {

    PageData<DeviceRepairEntity> page(int pageNum, int pageSize, Long labId, String labType, Long deviceId, Integer status);

    DeviceRepairEntity create(DeviceRepairCreateRequest request);

    DeviceRepairEntity getById(Long id);

    DeviceRepairEntity updateStatus(Long id, DeviceRepairStatusUpdateRequest request);
}

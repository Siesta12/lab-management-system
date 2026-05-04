package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.device.DeviceCreateRequest;
import com.nlt.domain.dto.device.DeviceUpdateRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface DeviceService {

    PageData<DeviceEntity> page(int pageNum, int pageSize, Long labId, String labType, String deviceName, String deviceCode, Integer status);

    DeviceEntity create(DeviceCreateRequest request);

    List<OptionItem> options(Long labId);

    DeviceEntity getById(Long id);

    DeviceEntity update(Long id, DeviceUpdateRequest request);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}

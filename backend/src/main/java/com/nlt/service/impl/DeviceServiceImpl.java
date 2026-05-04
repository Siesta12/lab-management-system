package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.device.DeviceCreateRequest;
import com.nlt.domain.dto.device.DeviceUpdateRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.DeviceMapper;
import com.nlt.mapper.DeviceRepairMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.service.DeviceService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceRepairMapper deviceRepairMapper;
    private final LabMapper labMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public PageData<DeviceEntity> page(int pageNum, int pageSize, Long labId, String labType, String deviceName, String deviceCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        Long departmentId = currentUserScopeService.requireCurrentDepartmentId();
        return new PageData<>(
            deviceMapper.selectPage(offset, pageSize, labId, trimToNull(labType), deviceName, deviceCode, status, departmentId),
            deviceMapper.countPage(labId, trimToNull(labType), deviceName, deviceCode, status, departmentId),
            pageNum,
            pageSize
        );
    }

    @Override
    public DeviceEntity create(DeviceCreateRequest request) {
        ensureAdmin();
        ensureLabAccessible(request.getLabId());
        DeviceEntity entity = toEntity(request);
        entity.setDeviceCode(nextDeviceCode());
        deviceMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public List<OptionItem> options(Long labId) {
        Long departmentId = currentUserScopeService.requireCurrentDepartmentId();
        return deviceMapper.selectOptions(labId, departmentId).stream()
            .map(item -> new OptionItem(item.getDeviceName(), item.getId()))
            .toList();
    }

    @Override
    public DeviceEntity getById(Long id) {
        DeviceEntity entity = deviceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "设备不存在");
        }
        ensureLabAccessible(entity.getLabId());
        return entity;
    }

    @Override
    public DeviceEntity update(Long id, DeviceUpdateRequest request) {
        ensureAdmin();
        DeviceEntity current = getById(id);
        ensureLabAccessible(request.getLabId());
        DeviceEntity entity = toEntity(request);
        ensureDeviceStatusEditable(id, entity.getStatus());
        entity.setId(id);
        entity.setDeviceCode(current.getDeviceCode());
        if (request.getAvailableQuantity() == null) {
            entity.setAvailableQuantity(current.getAvailableQuantity());
        }
        deviceMapper.update(entity);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        ensureAdmin();
        getById(id);
        deviceMapper.softDelete(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ensureAdmin();
        getById(id);
        ensureDeviceStatusEditable(id, status);
        deviceMapper.updateStatus(id, status);
    }

    private DeviceEntity toEntity(DeviceCreateRequest request) {
        DeviceEntity entity = new DeviceEntity();
        BeanUtils.copyProperties(request, entity);
        normalizeDeviceEntity(entity, request.getPurchaseDate());
        return entity;
    }

    private DeviceEntity toEntity(DeviceUpdateRequest request) {
        DeviceEntity entity = new DeviceEntity();
        BeanUtils.copyProperties(request, entity);
        normalizeDeviceEntity(entity, request.getPurchaseDate());
        return entity;
    }

    private void normalizeDeviceEntity(DeviceEntity entity, String purchaseDate) {
        if (purchaseDate != null && !purchaseDate.isBlank()) {
            entity.setPurchaseDate(LocalDate.parse(purchaseDate));
        }
        if (entity.getQuantity() == null || entity.getQuantity() <= 0) {
            entity.setQuantity(1);
        }
        if (entity.getAvailableQuantity() == null) {
            entity.setAvailableQuantity(entity.getQuantity());
        }
        if (entity.getAvailableQuantity() > entity.getQuantity()) {
            entity.setAvailableQuantity(entity.getQuantity());
        }
        if (entity.getAvailableQuantity() < 0) {
            entity.setAvailableQuantity(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
    }

    private String nextDeviceCode() {
        for (int i = 0; i < 5; i++) {
            String candidate = "D" + System.currentTimeMillis();
            if (deviceMapper.countByDeviceCode(candidate) == 0) {
                return candidate;
            }
        }
        throw new BusinessException(500, "设备编号生成失败，请稍后重试");
    }

    private void ensureLabAccessible(Long labId) {
        if (labId == null) {
            throw new BusinessException(400, "实验室不能为空");
        }
        LabEntity lab = labMapper.selectById(labId);
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "实验室不存在");
        }
        currentUserScopeService.ensureCurrentDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
    }

    private void ensureAdmin() {
        if (!currentUserScopeService.isAdmin()) {
            throw new BusinessException(403, "无权修改设备信息");
        }
    }

    private void ensureDeviceStatusEditable(Long deviceId, Integer targetStatus) {
        if (deviceId == null || targetStatus == null || targetStatus == 2) {
            return;
        }
        if (deviceRepairMapper.countActiveByDeviceId(deviceId, null) > 0) {
            throw new BusinessException(400, "当前设备存在未完成报修，设备状态只能保持为维修中");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

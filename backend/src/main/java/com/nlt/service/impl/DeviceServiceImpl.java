package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.device.DeviceSaveRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.DeviceMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.service.DeviceService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
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
    public DeviceEntity create(DeviceSaveRequest request) {
        ensureAdmin();
        ensureLabAccessible(request.getLabId());
        DeviceEntity entity = toEntity(request);
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
    public DeviceEntity update(Long id, DeviceSaveRequest request) {
        ensureAdmin();
        DeviceEntity current = getById(id);
        ensureLabAccessible(request.getLabId());
        DeviceEntity entity = toEntity(request);
        entity.setId(id);
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
        deviceMapper.updateStatus(id, status);
    }

    private DeviceEntity toEntity(DeviceSaveRequest request) {
        DeviceEntity entity = new DeviceEntity();
        BeanUtils.copyProperties(request, entity);
        if (request.getPurchaseDate() != null && !request.getPurchaseDate().isBlank()) {
            entity.setPurchaseDate(LocalDate.parse(request.getPurchaseDate()));
        }
        if (entity.getQuantity() == null) {
            entity.setQuantity(1);
        }
        if (entity.getQuantity() <= 0) {
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
        return entity;
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

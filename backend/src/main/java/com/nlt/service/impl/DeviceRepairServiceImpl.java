package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.device.DeviceRepairCreateRequest;
import com.nlt.domain.dto.device.DeviceRepairStatusUpdateRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.entity.DeviceRepairEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.mapper.DeviceMapper;
import com.nlt.mapper.DeviceRepairMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.service.DeviceRepairService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceRepairServiceImpl implements DeviceRepairService {

    private final DeviceRepairMapper deviceRepairMapper;
    private final DeviceMapper deviceMapper;
    private final LabMapper labMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public PageData<DeviceRepairEntity> page(int pageNum, int pageSize, Long labId, String labType, Long deviceId, Integer status) {
        ensureTeacherOrAdmin();
        int offset = (pageNum - 1) * pageSize;
        Long departmentId = currentUserScopeService.requireCurrentDepartmentId();
        return new PageData<>(
            deviceRepairMapper.selectPage(offset, pageSize, labId, trimToNull(labType), deviceId, status, departmentId, null),
            deviceRepairMapper.countPage(labId, trimToNull(labType), deviceId, status, departmentId, null),
            pageNum,
            pageSize
        );
    }

    @Transactional
    @Override
    public DeviceRepairEntity create(DeviceRepairCreateRequest request) {
        ensureTeacherOrAdmin();
        DeviceEntity device = deviceMapper.selectById(request.getDeviceId());
        if (device == null || device.getLabId() == null) {
            throw new BusinessException(404, "设备不存在");
        }
        LabEntity lab = labMapper.selectById(device.getLabId());
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "设备不存在");
        }
        currentUserScopeService.ensureCurrentDepartmentAccessible(lab.getDepartmentId(), "设备不存在");
        if (deviceRepairMapper.countActiveByDeviceId(request.getDeviceId(), null) > 0) {
            throw new BusinessException(400, "当前设备已有未完成报修，请勿重复提交");
        }

        DeviceRepairEntity entity = new DeviceRepairEntity();
        entity.setDeviceId(request.getDeviceId());
        entity.setLabId(device.getLabId());
        entity.setApplicantUserId(currentUserScopeService.currentUserIdOrNull());
        entity.setIssueDescription(request.getIssueDescription().trim());
        int urgencyLevel = request.getUrgencyLevel() == null ? 2 : request.getUrgencyLevel();
        if (urgencyLevel < 1 || urgencyLevel > 3) {
            urgencyLevel = 2;
        }
        entity.setUrgencyLevel(urgencyLevel);
        entity.setStatus(1);
        deviceRepairMapper.insert(entity);
        deviceMapper.updateStatus(request.getDeviceId(), 2);
        return getById(entity.getId());
    }

    @Override
    public DeviceRepairEntity getById(Long id) {
        DeviceRepairEntity entity = deviceRepairMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "报修单不存在");
        }
        ensureRepairVisible(entity);
        return entity;
    }

    @Transactional
    @Override
    public DeviceRepairEntity updateStatus(Long id, DeviceRepairStatusUpdateRequest request) {
        ensureAdmin();
        if (request.getStatus() == null || request.getStatus() < 1 || request.getStatus() > 4) {
            throw new BusinessException(400, "报修状态不合法");
        }
        DeviceRepairEntity entity = getById(id);
        entity.setStatus(request.getStatus());
        entity.setHandlerUserId(currentUserScopeService.currentUserIdOrNull());
        if (request.getHandlingResult() != null) {
            entity.setHandlingResult(request.getHandlingResult().trim());
        }
        if (request.getStatus() == 3 || request.getStatus() == 4) {
            entity.setHandledAt(LocalDateTime.now());
        } else {
            entity.setHandledAt(null);
        }
        deviceRepairMapper.updateStatus(entity);
        deviceMapper.updateStatus(entity.getDeviceId(), resolveDeviceStatus(entity, request));
        return getById(id);
    }

    private Integer resolveDeviceStatus(DeviceRepairEntity entity, DeviceRepairStatusUpdateRequest request) {
        long otherActiveCount = deviceRepairMapper.countActiveByDeviceId(entity.getDeviceId(), entity.getId());
        if (request.getStatus() == 1 || request.getStatus() == 2 || otherActiveCount > 0) {
            return 2;
        }
        if (request.getStatus() == 4) {
            return 1;
        }
        if (request.getDeviceStatus() == null || (request.getDeviceStatus() != 1 && request.getDeviceStatus() != 3)) {
            throw new BusinessException(400, "完成报修时请明确设备恢复正常或禁用");
        }
        return request.getDeviceStatus();
    }

    private void ensureRepairVisible(DeviceRepairEntity entity) {
        if (!currentUserScopeService.isTeacher() && !currentUserScopeService.isAdmin()) {
            throw new BusinessException(403, "无权查看报修单");
        }
        LabEntity lab = labMapper.selectById(entity.getLabId());
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "报修单不存在");
        }
        currentUserScopeService.ensureCurrentDepartmentAccessible(lab.getDepartmentId(), "报修单不存在");
    }

    private void ensureTeacherOrAdmin() {
        if (!currentUserScopeService.isTeacher() && !currentUserScopeService.isAdmin()) {
            throw new BusinessException(403, "无权操作设备报修");
        }
    }

    private void ensureAdmin() {
        if (!currentUserScopeService.isAdmin()) {
            throw new BusinessException(403, "无权处理报修");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

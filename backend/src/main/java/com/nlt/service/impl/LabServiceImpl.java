package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.LabMapper;
import com.nlt.service.LabService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabMapper labMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public PageData<LabEntity> page(int pageNum, int pageSize, Long labId, String labName, String labCode, String labType,
        Long departmentId, Integer openStatus, Integer labStatus) {
        int offset = (pageNum - 1) * pageSize;
        Long scopedDepartmentId = currentUserScopeService.resolveDepartmentFilter(departmentId);
        return new PageData<>(
            labMapper.selectPage(offset, pageSize, labId, labName, labCode, labType, scopedDepartmentId, openStatus, labStatus),
            labMapper.countPage(labId, labName, labCode, labType, scopedDepartmentId, openStatus, labStatus),
            pageNum,
            pageSize
        );
    }

    @Override
    public LabEntity create(LabSaveRequest request) {
        LabEntity entity = new LabEntity();
        BeanUtils.copyProperties(request, entity);
        Long scopedDepartmentId = currentUserScopeService.resolveAdminDepartmentId();
        if (scopedDepartmentId != null) {
            entity.setDepartmentId(scopedDepartmentId);
        }
        if (entity.getOpenStatus() == null) {
            entity.setOpenStatus(1);
        }
        if (entity.getLabStatus() == null) {
            entity.setLabStatus(1);
        }
        labMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public LabEntity getById(Long id) {
        LabEntity entity = labMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "实验室不存在");
        }
        currentUserScopeService.ensureDepartmentAccessible(entity.getDepartmentId(), "实验室不存在");
        return entity;
    }

    @Override
    public LabEntity update(Long id, LabSaveRequest request) {
        LabEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        Long scopedDepartmentId = currentUserScopeService.resolveAdminDepartmentId();
        if (scopedDepartmentId != null) {
            entity.setDepartmentId(scopedDepartmentId);
        }
        labMapper.update(entity);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        labMapper.softDelete(id);
    }

    @Override
    public void updateOpenStatus(Long id, Integer status) {
        getById(id);
        labMapper.updateOpenStatus(id, status);
    }

    @Override
    public void updateLabStatus(Long id, Integer status) {
        getById(id);
        labMapper.updateLabStatus(id, status);
    }

    @Override
    public List<OptionItem> options(Integer openStatus, Long departmentId) {
        Long scopedDepartmentId = currentUserScopeService.resolveDepartmentFilter(departmentId);
        return labMapper.selectOptions(openStatus, scopedDepartmentId).stream()
            .map(item -> new OptionItem(item.getLabName(), item.getId()))
            .toList();
    }
}

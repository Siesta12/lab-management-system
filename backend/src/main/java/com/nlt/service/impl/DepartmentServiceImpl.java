package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.department.DepartmentSaveRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.DepartmentMapper;
import com.nlt.service.DepartmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    public PageData<DepartmentEntity> page(int pageNum, int pageSize, String departmentName, String departmentCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
                departmentMapper.selectPage(offset, pageSize, departmentName, departmentCode, status),
                departmentMapper.countPage(departmentName, departmentCode, status),
                pageNum,
                pageSize
        );
    }

    @Override
    public DepartmentEntity create(DepartmentSaveRequest request) {
        DepartmentEntity entity = new DepartmentEntity();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        departmentMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public DepartmentEntity getById(Long id) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "部门不存在");
        }
        return entity;
    }

    @Override
    public DepartmentEntity update(Long id, DepartmentSaveRequest request) {
        DepartmentEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        departmentMapper.update(entity);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        departmentMapper.softDelete(id);
    }

    @Override
    public List<OptionItem> options() {
        return departmentMapper.selectOptions().stream()
                .map(item -> new OptionItem(item.getDepartmentName(), item.getId()))
                .toList();
    }

}


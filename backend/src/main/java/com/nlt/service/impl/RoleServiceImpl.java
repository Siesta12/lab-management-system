package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.role.RoleSaveRequest;
import com.nlt.domain.entity.RoleEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.RoleMapper;
import com.nlt.service.RoleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public PageData<RoleEntity> page(int pageNum, int pageSize, String roleName, String roleCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
        roleMapper.selectPage(offset, pageSize, roleName, roleCode, status),
        roleMapper.countPage(roleName, roleCode, status),
        pageNum,
        pageSize
        );
    }

    @Override
    public RoleEntity create(RoleSaveRequest request) {
        RoleEntity entity = new RoleEntity();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        roleMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public RoleEntity getById(Long id) {
        RoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return entity;
    }

    @Override
    public RoleEntity update(Long id, RoleSaveRequest request) {
        RoleEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        roleMapper.update(entity);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        roleMapper.softDelete(id);
    }

    @Override
    public List<OptionItem> options() {
        return roleMapper.selectOptions().stream()
        .map(item -> new OptionItem(item.getRoleName(), item.getId()))
        .toList();
    }

}


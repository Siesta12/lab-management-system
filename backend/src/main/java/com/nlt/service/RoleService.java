package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.role.RoleSaveRequest;
import com.nlt.domain.entity.RoleEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface RoleService {

    PageData<RoleEntity> page(int pageNum, int pageSize, String roleName, String roleCode, Integer status);

    RoleEntity create(RoleSaveRequest request);

    RoleEntity getById(Long id);

    RoleEntity update(Long id, RoleSaveRequest request);

    void delete(Long id);

    List<OptionItem> options();

}


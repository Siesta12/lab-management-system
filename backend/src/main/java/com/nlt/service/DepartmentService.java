package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.department.DepartmentSaveRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface DepartmentService {

    PageData<DepartmentEntity> page(int pageNum, int pageSize, String departmentName, String departmentCode, Integer status);

    DepartmentEntity create(DepartmentSaveRequest request);

    DepartmentEntity getById(Long id);

    DepartmentEntity update(Long id, DepartmentSaveRequest request);

    void delete(Long id);

    List<OptionItem> options();

}


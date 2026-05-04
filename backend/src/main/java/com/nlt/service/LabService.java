package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface LabService {

    PageData<LabEntity> page(int pageNum, int pageSize, Long labId, String labName, String labCode, String labType,
        Long departmentId, Integer openStatus, Integer labStatus);

    LabEntity create(LabSaveRequest request);

    LabEntity getById(Long id);

    LabEntity update(Long id, LabSaveRequest request);

    void delete(Long id);

    void updateOpenStatus(Long id, Integer status);

    void updateLabStatus(Long id, Integer status);

    List<OptionItem> options(Integer openStatus, Long departmentId);

}


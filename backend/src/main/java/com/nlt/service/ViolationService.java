package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.violation.ViolationSaveRequest;
import com.nlt.domain.entity.ViolationRecordEntity;

public interface ViolationService {

    PageData<ViolationRecordEntity> page(int pageNum, int pageSize, Long userId, Long reservationId, Integer violationType);

    ViolationRecordEntity create(ViolationSaveRequest request);

    ViolationRecordEntity getById(Long id);

    PageData<ViolationRecordEntity> mine(Long userId, int pageNum, int pageSize, Integer violationType, Integer scoreDirection);

    void delete(Long id);

}


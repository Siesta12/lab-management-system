package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.entity.ExperimentReportConsumableEntity;
import java.util.List;

public interface ConsumableService {

    PageData<ConsumableEntity> page(int pageNum, int pageSize, Long labId, String labType, String consumableName, String consumableCode,
        Integer status, Boolean warningOnly);

    List<ConsumableEntity> availableOptions(Long labId);

    ConsumableEntity create(ConsumableSaveRequest request);

    ConsumableEntity getById(Long id);

    ConsumableEntity update(Long id, ConsumableSaveRequest request);

    void delete(Long id);

    PageData<ConsumableEntity> warningList(int pageNum, int pageSize);

    ConsumableEntity updateStock(Long id, ConsumableStockUpdateRequest request, Long operatorUserId);

    PageData<ExperimentReportConsumableEntity> usagePage(int pageNum, int pageSize, Integer status, Long labId,
        String keyword);

}


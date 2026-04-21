package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.entity.ConsumableStockLogEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.mapper.ConsumableMapper;
import com.nlt.mapper.ConsumableStockLogMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.service.ConsumableService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsumableServiceImpl implements ConsumableService {

    private final ConsumableMapper consumableMapper;
    private final ConsumableStockLogMapper consumableStockLogMapper;
    private final LabMapper labMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public PageData<ConsumableEntity> page(int pageNum, int pageSize, Long labId, String consumableName, String consumableCode) {
        int offset = (pageNum - 1) * pageSize;
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        return new PageData<>(
            consumableMapper.selectPage(offset, pageSize, labId, consumableName, consumableCode, departmentId),
            consumableMapper.countPage(labId, consumableName, consumableCode, departmentId),
            pageNum,
            pageSize
        );
    }

    @Override
    public ConsumableEntity create(ConsumableSaveRequest request) {
        ensureLabAccessible(request.getLabId());
        ConsumableEntity entity = new ConsumableEntity();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStockQuantity() == null) {
            entity.setStockQuantity(0);
        }
        if (entity.getWarningThreshold() == null) {
            entity.setWarningThreshold(0);
        }
        consumableMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public ConsumableEntity getById(Long id) {
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "耗材不存在");
        }
        ensureLabAccessible(entity.getLabId());
        return entity;
    }

    @Override
    public ConsumableEntity update(Long id, ConsumableSaveRequest request) {
        ConsumableEntity entity = getById(id);
        ensureLabAccessible(request.getLabId());
        BeanUtils.copyProperties(request, entity);
        consumableMapper.update(entity);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        consumableMapper.softDelete(id);
    }

    @Override
    public PageData<ConsumableEntity> warningList(int pageNum, int pageSize) {
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        var list = consumableMapper.selectWarningList(departmentId);
        int fromIndex = Math.min((pageNum - 1) * pageSize, list.size());
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return new PageData<>(list.subList(fromIndex, toIndex), list.size(), pageNum, pageSize);
    }

    @Transactional
    @Override
    public ConsumableEntity updateStock(Long id, ConsumableStockUpdateRequest request, Long operatorUserId) {
        ConsumableEntity entity = getById(id);
        int beforeStock = entity.getStockQuantity();
        int afterStock = request.getStockQuantity();
        if (afterStock < 0) {
            throw new BusinessException(400, "库存数量不能小于 0");
        }
        consumableMapper.updateStock(id, afterStock);
        ConsumableStockLogEntity logEntity = new ConsumableStockLogEntity();
        logEntity.setConsumableId(id);
        logEntity.setChangeType((request.getChangeType() == null || request.getChangeType().isBlank()) ? "ADJUST" : request.getChangeType());
        logEntity.setChangeAmount(afterStock - beforeStock);
        logEntity.setBeforeStock(beforeStock);
        logEntity.setAfterStock(afterStock);
        logEntity.setOperatorUserId(operatorUserId);
        logEntity.setRemark(request.getRemark());
        consumableStockLogMapper.insert(logEntity);
        return getById(id);
    }

    private void ensureLabAccessible(Long labId) {
        if (labId == null) {
            throw new BusinessException(400, "实验室不能为空");
        }
        LabEntity lab = labMapper.selectById(labId);
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "实验室不存在");
        }
        currentUserScopeService.ensureDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
    }
}

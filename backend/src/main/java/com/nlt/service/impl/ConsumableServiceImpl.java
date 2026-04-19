package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.entity.ConsumableStockLogEntity;
import com.nlt.mapper.ConsumableMapper;
import com.nlt.mapper.ConsumableStockLogMapper;
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

    /**
     * 查询耗材信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param consumableName 参数
     * @param consumableCode 参数
     * @return 分页数据
     */
    @Override
    public PageData<ConsumableEntity> page(int pageNum, int pageSize, Long labId, String consumableName, String consumableCode) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
        consumableMapper.selectPage(offset, pageSize, labId, consumableName, consumableCode),
        consumableMapper.countPage(labId, consumableName, consumableCode),
        pageNum,
        pageSize
        );
    }

    /**
     * 新增耗材信息
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public ConsumableEntity create(ConsumableSaveRequest request) {
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

    /**
     * 查询耗材信息
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public ConsumableEntity getById(Long id) {
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "耗材不存在");
        }
        return entity;
    }

    /**
     * 更新耗材信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public ConsumableEntity update(Long id, ConsumableSaveRequest request) {
        ConsumableEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        consumableMapper.update(entity);
        return getById(id);
    }

    /**
     * 删除耗材信息
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        consumableMapper.softDelete(id);
    }

    /**
     * 处理耗材信息
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    @Override
    public PageData<ConsumableEntity> warningList(int pageNum, int pageSize) {
        var list = consumableMapper.selectWarningList();
        int fromIndex = Math.min((pageNum - 1) * pageSize, list.size());
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return new PageData<>(list.subList(fromIndex, toIndex), list.size(), pageNum, pageSize);
    }

    /**
     * 更新耗材信息
     * @param id 主键ID
     * @param request 请求参数
     * @param operatorUserId 操作人用户ID
     * @return 处理结果
     */
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

}

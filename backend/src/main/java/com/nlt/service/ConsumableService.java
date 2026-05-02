package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.entity.ExperimentReportConsumableEntity;
import java.util.List;

public interface ConsumableService {

    /**
     * 查询耗材信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param consumableName 参数
     * @param consumableCode 参数
     * @return 分页数据
     */
    PageData<ConsumableEntity> page(int pageNum, int pageSize, Long labId, String labType, String consumableName, String consumableCode,
        Integer status);

    List<ConsumableEntity> availableOptions(Long labId);

    /**
     * 新增耗材信息
     * @param request 请求参数
     * @return 处理结果
     */
    ConsumableEntity create(ConsumableSaveRequest request);

    /**
     * 查询耗材信息
     * @param id 主键ID
     * @return 处理结果
     */
    ConsumableEntity getById(Long id);

    /**
     * 更新耗材信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    ConsumableEntity update(Long id, ConsumableSaveRequest request);

    /**
     * 删除耗材信息
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 处理耗材信息
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    PageData<ConsumableEntity> warningList(int pageNum, int pageSize);

    /**
     * 更新耗材信息
     * @param id 主键ID
     * @param request 请求参数
     * @param operatorUserId 操作人用户ID
     * @return 处理结果
     */
    ConsumableEntity updateStock(Long id, ConsumableStockUpdateRequest request, Long operatorUserId);

    PageData<ExperimentReportConsumableEntity> usagePage(int pageNum, int pageSize, Integer status, Long labId,
        String keyword);

}


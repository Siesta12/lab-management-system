package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.violation.ViolationSaveRequest;
import com.nlt.domain.entity.ViolationRecordEntity;

public interface ViolationService {

    /**
     * 查询违规记录列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @param violationType 违规类型
     * @return 分页数据
     */
    PageData<ViolationRecordEntity> page(int pageNum, int pageSize, Long userId, Long reservationId, Integer violationType);

    /**
     * 新增违规记录
     * @param request 请求参数
     * @return 处理结果
     */
    ViolationRecordEntity create(ViolationSaveRequest request);

    /**
     * 查询违规记录
     * @param id 主键ID
     * @return 处理结果
     */
    ViolationRecordEntity getById(Long id);

    /**
     * 查询当前用户预约信息列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    PageData<ViolationRecordEntity> mine(Long userId, int pageNum, int pageSize, Integer violationType, Integer scoreDirection);

    /**
     * 删除违规记录
     * @param id 主键ID
     */
    void delete(Long id);

}


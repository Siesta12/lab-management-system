package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import java.util.List;

public interface ReservationAuditLogService {

    /**
     * 查询预约审核日志列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param reservationId 预约ID
     * @param auditUserId 审核用户ID
     * @return 分页数据
     */
    PageData<ReservationAuditLogEntity> page(int pageNum, int pageSize, Long reservationId, Long auditUserId);

    /**
     * 根据预约ID查询审核日志
     * @param reservationId 预约ID
     * @return 数据列表
     */
    List<ReservationAuditLogEntity> byReservationId(Long reservationId);

    /**
     * 根据ID查询预约审核日志
     * @param id 主键ID
     * @return 审核日志实体
     */
    ReservationAuditLogEntity getById(Long id);

}


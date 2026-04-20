package com.nlt.mapper;

import com.nlt.domain.entity.ReservationAuditLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationAuditLogMapper {

    /**
     * 查询预约审核日志
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param reservationId 预约ID
     * @param auditUserId 审核人ID
     * @return 数据列表
     */
    List<ReservationAuditLogEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("reservationId") Long reservationId,
    @Param("auditUserId") Long auditUserId);

    /**
     * 统计预约审核日志数量
     * @param reservationId 预约ID
     * @param auditUserId 审核人ID
     * @return 处理结果
     */
    long countPage(@Param("reservationId") Long reservationId, @Param("auditUserId") Long auditUserId);

    /**
     * 查询预约审核日志
     * @param id 主键ID
     * @return 处理结果
     */
    ReservationAuditLogEntity selectById(@Param("id") Long id);

    /**
     * 查询预约审核日志
     * @param reservationId 预约ID
     * @return 数据列表
     */
    List<ReservationAuditLogEntity> selectByReservationId(@Param("reservationId") Long reservationId);

    /**
     * 新增预约审核日志
     * @param entity 参数
     * @return 处理结果
     */
    int insert(ReservationAuditLogEntity entity);

}


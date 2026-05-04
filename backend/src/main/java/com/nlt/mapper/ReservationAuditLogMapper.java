package com.nlt.mapper;

import com.nlt.domain.entity.ReservationAuditLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationAuditLogMapper {

    List<ReservationAuditLogEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("reservationId") Long reservationId,
    @Param("auditUserId") Long auditUserId);

    long countPage(@Param("reservationId") Long reservationId, @Param("auditUserId") Long auditUserId);

    ReservationAuditLogEntity selectById(@Param("id") Long id);

    List<ReservationAuditLogEntity> selectByReservationId(@Param("reservationId") Long reservationId);

    int insert(ReservationAuditLogEntity entity);

}


package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import java.util.List;

public interface ReservationAuditLogService {

    PageData<ReservationAuditLogEntity> page(int pageNum, int pageSize, Long reservationId, Long auditUserId);

    List<ReservationAuditLogEntity> byReservationId(Long reservationId);

    ReservationAuditLogEntity getById(Long id);

}


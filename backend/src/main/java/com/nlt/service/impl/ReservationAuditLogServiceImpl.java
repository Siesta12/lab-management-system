package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import com.nlt.mapper.ReservationAuditLogMapper;
import com.nlt.service.ReservationAuditLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationAuditLogServiceImpl implements ReservationAuditLogService {

    private final ReservationAuditLogMapper reservationAuditLogMapper;

    @Override
    public PageData<ReservationAuditLogEntity> page(int pageNum, int pageSize, Long reservationId, Long auditUserId) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
                reservationAuditLogMapper.selectPage(offset, pageSize, reservationId, auditUserId),
                reservationAuditLogMapper.countPage(reservationId, auditUserId),
                pageNum,
                pageSize
        );
    }

    @Override
    public List<ReservationAuditLogEntity> byReservationId(Long reservationId) {
        return reservationAuditLogMapper.selectByReservationId(reservationId);
    }

    @Override
    public ReservationAuditLogEntity getById(Long id) {
        ReservationAuditLogEntity entity = reservationAuditLogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "预约审核日志不存在");
        }
        return entity;
    }

}


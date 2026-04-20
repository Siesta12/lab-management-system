package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservationAuditLogEntity {

    private Long id;

    private Long reservationId;

    private Long auditUserId;

    private Integer auditAction;

    private String auditComment;

    private LocalDateTime createdAt;

}


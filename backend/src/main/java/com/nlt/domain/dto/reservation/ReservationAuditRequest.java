package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationAuditRequest {

    @NotNull
    private Integer auditAction;

    private String auditComment;

    private String rejectReason;

}

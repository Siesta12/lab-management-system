package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReservationRejectRequest {

    @NotBlank
    private String rejectReason;

    private String auditComment;
}


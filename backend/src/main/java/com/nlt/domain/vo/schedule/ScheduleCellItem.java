package com.nlt.domain.vo.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScheduleCellItem {

    private Long periodId;

    private String status; // FREE / RESERVED / PENDING / MAINTENANCE / CLOSED

    private Long reservationId;

    private String reservationNo;

    private Integer reservationStatus;

    private Long maintenanceId;

    private String maintenanceReason;
}


package com.nlt.domain.vo.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScheduleCellItem {

    private Long periodId;

    private String status;

    private Long reservationId;

    private String reservationNo;

    private Integer reservationStatus;

    private Long maintenanceId;

    private String maintenanceReason;

    private String note;
}


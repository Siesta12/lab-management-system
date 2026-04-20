package com.nlt.domain.vo.schedule;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ReservedSlotRow {

    private LocalDate reservationDate;

    private Long periodId;

    private Long reservationId;

    private String reservationNo;

    private Integer reservationStatus;
}


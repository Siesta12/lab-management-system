package com.nlt.domain.vo.schedule;

import lombok.Data;

@Data
public class ReservedSlotRowWithLab {

    private Long labId;

    private Long periodId;

    private Long reservationId;

    private String reservationNo;

    private Integer reservationStatus;
}


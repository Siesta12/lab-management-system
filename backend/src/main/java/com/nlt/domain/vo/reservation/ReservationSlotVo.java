package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReservationSlotVo {

    private Long id;

    private Long reservationId;

    private String reservationDate;

    private Integer weekday;

    private Long periodId;

    private Integer periodNo;

    private String periodName;

    private Integer slotStatus;
}

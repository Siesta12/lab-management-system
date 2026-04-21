package com.nlt.domain.vo.reservation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationConflictSlotVo {

    private Long labId;

    private String labName;

    private String reservationDate;

    private Integer weekday;

    private Long periodId;

    private String periodName;

    private String startTime;

    private String endTime;

    private Integer conflictCount;

    private List<ReservationConflictReservationVo> reservations;
}

package com.nlt.domain.vo.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
public class ReservationCheckCandidateVo {

    private Long reservationId;

    private String reservationNo;

    private Long labId;

    private String labName;

    private Long applicantUserId;

    private LocalDate reservationDate;

    private Long periodId;

    private String periodName;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalDateTime checkInTime;
}

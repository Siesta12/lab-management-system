package com.nlt.domain.vo.reservation;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ReservationConflictRowVo {

    private Long labId;

    private String labName;

    private LocalDate reservationDate;

    private Long periodId;

    private String periodName;

    private String startTime;

    private String endTime;

    private Long reservationId;

    private String reservationNo;

    private Integer reservationStatus;

    private Long applicantUserId;

    private String applicantName;

    private Integer reservationType;

    private Integer priorityLevel;

    private String createdAt;

    private String usagePurpose;

    private String courseOrProjectName;
}

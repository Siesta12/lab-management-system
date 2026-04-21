package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationConflictReservationVo {

    private Long reservationId;

    private String reservationNo;

    private Long applicantUserId;

    private String applicantName;

    private Integer reservationType;

    private Integer priorityLevel;

    private Integer status;

    private String createdAt;

    private String usagePurpose;

    private String courseOrProjectName;
}

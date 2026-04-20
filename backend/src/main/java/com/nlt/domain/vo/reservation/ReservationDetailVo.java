package com.nlt.domain.vo.reservation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailVo {

    private Long id;

    private String reservationNo;

    private Long labId;

    private Long applicantUserId;

    private Long approverUserId;

    private Integer reservationType;

    private Integer priorityLevel;

    private String usagePurpose;

    private String courseOrProjectName;

    private Integer participantCount;

    private String contactPhone;

    private Integer status;

    private String rejectReason;

    private String checkInTime;

    private String checkOutTime;

    private String createdAt;

    private String updatedAt;

    private List<ReservationSlotVo> slots;
}


package com.nlt.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservationEntity {

    private Long id;

    private String reservationNo;

    private Long labId;

    private Long applicantUserId;

    private Long approverUserId;

    private Integer reservationType;

    private Integer priorityLevel;

    private LocalDate reservationDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String usagePurpose;

    private String courseOrProjectName;

    private Integer participantCount;

    private String contactPhone;

    private Integer status;

    private String rejectReason;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}

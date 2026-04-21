package com.nlt.domain.vo.dashboard;

import lombok.Data;

@Data
public class DashboardReservationItemVo {

    private Long reservationId;

    private String reservationNo;

    private Long labId;

    private String labName;

    private Long applicantUserId;

    private String applicantName;

    private Integer applicantCreditScore;

    private Integer reservationType;

    private Integer priorityLevel;

    private Integer status;

    private String reservationDate;

    private Long periodId;

    private Integer periodNo;

    private String periodName;

    private String startTime;

    private String endTime;

    private String createdAt;

    private String timeRange;

    private String typeLabel;

    private String statusLabel;

    private String note;
}

package com.nlt.domain.vo.statistics.export;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservationExportVo {

    private String reservationNo;

    private String labName;

    private String labType;

    private String applicantName;

    private String userNo;

    private String userRole;

    private Integer reservationType;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer lateViolation;

    private Integer noShowViolation;

    private LocalDateTime createdAt;

    private String approverName;

    private LocalDateTime auditedAt;
}

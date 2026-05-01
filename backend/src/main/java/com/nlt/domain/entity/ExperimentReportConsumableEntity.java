package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentReportConsumableEntity {

    private Long id;

    private Long reportId;

    private Long consumableId;

    private Long labId;

    private String consumableName;

    private String specification;

    private Integer quantity;

    private String unit;

    private Integer status;

    private Long confirmUserId;

    private LocalDateTime confirmedAt;

    private String rejectReason;

    private Long stockLogId;

    private String remark;

    private LocalDateTime createdAt;

    private String reportNo;

    private String experimentName;

    private Integer reportStatus;

    private LocalDateTime submittedAt;

    private String studentName;

    private String teacherName;

    private String labName;

    private String confirmUserName;
}

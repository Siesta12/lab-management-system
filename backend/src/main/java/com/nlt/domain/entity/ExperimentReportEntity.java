package com.nlt.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ExperimentReportEntity {

    private Long id;

    private String reportNo;

    private Long studentId;

    private Long teacherId;

    private Long departmentId;

    private Long labId;

    private Long reservationId;

    private String title;

    private String experimentName;

    private LocalDate experimentDate;

    private String purpose;

    private String principle;

    private String steps;

    private String resultData;

    private String analysis;

    private String conclusion;

    private Integer status;

    private String teacherComment;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String studentName;

    private String studentNo;

    private String teacherName;

    private String labName;

    private String departmentName;

    private List<ExperimentReportConsumableEntity> consumables = new ArrayList<>();
}

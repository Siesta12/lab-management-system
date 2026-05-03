package com.nlt.domain.vo.statistics.export;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentReportExportVo {

    private String reportNo;

    private String reservationNo;

    private String labName;

    private String experimentName;

    private String submitterName;

    private String userNo;

    private LocalDateTime submittedAt;

    private Integer status;

    private String reviewerName;

    private LocalDateTime reviewedAt;

    private String teacherComment;
}

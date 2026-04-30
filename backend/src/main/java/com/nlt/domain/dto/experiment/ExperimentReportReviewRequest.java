package com.nlt.domain.dto.experiment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentReportReviewRequest {

    @NotNull
    private Integer status;

    private String teacherComment;
}

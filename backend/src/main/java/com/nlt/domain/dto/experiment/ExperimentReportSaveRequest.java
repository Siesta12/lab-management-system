package com.nlt.domain.dto.experiment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ExperimentReportSaveRequest {

    @NotNull
    private Long teacherId;

    @NotNull
    private Long labId;

    private Long reservationId;

    private String title;

    private String experimentName;

    @NotNull
    private LocalDate experimentDate;

    private String purpose;

    private String principle;

    private String steps;

    private String resultData;

    private String analysis;

    private String conclusion;

    @Valid
    private List<ExperimentReportConsumableRequest> consumables = new ArrayList<>();
}

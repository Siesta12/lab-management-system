package com.nlt.domain.dto.experiment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentReportConsumableRequest {

    @NotNull
    private Long consumableId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String remark;
}

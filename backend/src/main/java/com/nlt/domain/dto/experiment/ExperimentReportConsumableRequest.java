package com.nlt.domain.dto.experiment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExperimentReportConsumableRequest {

    @NotBlank
    private String consumableName;

    private String specification;

    @Min(0)
    private Integer quantity;

    private String unit;

    private String remark;
}

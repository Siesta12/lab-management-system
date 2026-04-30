package com.nlt.domain.entity;

import lombok.Data;

@Data
public class ExperimentReportConsumableEntity {

    private Long id;

    private Long reportId;

    private String consumableName;

    private String specification;

    private Integer quantity;

    private String unit;

    private String remark;
}

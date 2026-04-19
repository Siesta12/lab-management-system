package com.nlt.domain.dto.statistics;

import lombok.Data;

@Data
public class LabUsageStatisticsRequest {

    private String startDate;

    private String endDate;

    private Long departmentId;

    private Long labId;

}

package com.nlt.domain.dto.statistics;

import lombok.Data;

@Data
public class ViolationStatisticsRequest {

    private String startDate;

    private String endDate;

    private Long departmentId;

    private Integer violationType;

}

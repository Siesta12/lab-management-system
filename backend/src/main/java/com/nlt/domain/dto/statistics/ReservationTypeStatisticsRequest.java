package com.nlt.domain.dto.statistics;

import lombok.Data;

@Data
public class ReservationTypeStatisticsRequest {

    private String startDate;

    private String endDate;

    private Long labId;

    private Integer reservationType;

}


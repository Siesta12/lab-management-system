package com.nlt.domain.dto.statistics;

import lombok.Data;

@Data
public class ReservationStatusStatisticsRequest {

    private String startDate;

    private String endDate;

    private Long labId;

}


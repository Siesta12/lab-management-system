package com.nlt.domain.dto.statistics;

import java.time.LocalDate;
import lombok.Data;

@Data
public class AdminStatisticsQuery {

    private LocalDate startDate;

    private LocalDate endDate;

    private Long labId;

    private String labType;

    private Integer status;

    private Integer reservationType;

    private String exportType;
}

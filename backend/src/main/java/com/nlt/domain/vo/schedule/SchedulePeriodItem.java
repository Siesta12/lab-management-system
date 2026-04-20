package com.nlt.domain.vo.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchedulePeriodItem {

    private Long id;

    private Integer periodNo;

    private String periodName;

    private String startTime;

    private String endTime;
}



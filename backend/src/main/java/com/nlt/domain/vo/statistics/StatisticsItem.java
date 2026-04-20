package com.nlt.domain.vo.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatisticsItem {

    private String name;

    private Long count;

    private Double rate;

}


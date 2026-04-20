package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SlotRecommendationItem {

    private Long labId;

    private String labName;

    private String reservationDate;

    private Long periodId;

    private String periodName;

    private String recommendationReason;
}



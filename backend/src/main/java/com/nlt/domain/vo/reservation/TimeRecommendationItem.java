package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeRecommendationItem {

    private String startTime;

    private String endTime;

    private String reason;

}

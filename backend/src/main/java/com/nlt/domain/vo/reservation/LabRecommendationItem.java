package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabRecommendationItem {

    private Long labId;

    private String labCode;

    private String labName;

    private String buildingName;

    private String roomNo;

    private String availableStartTime;

    private String availableEndTime;

    private String reason;

}

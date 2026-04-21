package com.nlt.domain.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLabOccupancyVo {

    private String labType;

    private Integer occupiedSlots;

    private Integer totalOpenSlots;

    private Double occupancyRate;

    private Integer labCount;
}

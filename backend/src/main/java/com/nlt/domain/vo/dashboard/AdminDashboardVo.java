package com.nlt.domain.vo.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardVo {

    private List<DashboardCardVo> cards;

    private TodayReservationSummaryVo todayOverview;

    private List<DashboardReservationItemVo> todayTimeline;

    private List<DashboardSectionVo> pendingSections;

    private List<DashboardLabOccupancyVo> occupancyRates;

    private List<DashboardReservationItemVo> pendingReservations;
}

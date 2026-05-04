package com.nlt.service;

import com.nlt.domain.dto.statistics.LabUsageStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationStatusStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationTypeStatisticsRequest;
import com.nlt.domain.dto.statistics.TimeDistributionStatisticsRequest;
import com.nlt.domain.dto.statistics.ViolationStatisticsRequest;
import com.nlt.domain.vo.dashboard.AdminDashboardVo;
import com.nlt.domain.vo.statistics.StatisticsItem;
import java.util.List;
import java.util.Map;

public interface StatisticsService {

    Map<String, Long> overview();

    AdminDashboardVo adminOverview();

    List<StatisticsItem> labUsage(LabUsageStatisticsRequest request);

    List<StatisticsItem> reservationStatus(ReservationStatusStatisticsRequest request);

    List<StatisticsItem> reservationType(ReservationTypeStatisticsRequest request);

    List<StatisticsItem> timeDistribution(TimeDistributionStatisticsRequest request);

    List<StatisticsItem> violation(ViolationStatisticsRequest request);

    List<Map<String, Object>> reservationTrend(String startDate, String endDate);

}


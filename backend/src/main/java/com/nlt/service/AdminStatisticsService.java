package com.nlt.service;

import com.nlt.domain.dto.statistics.AdminStatisticsQuery;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ConsumableStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.CreditStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.DeviceStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.LabUsageStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OverviewVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ReservationStatsVo;
import jakarta.servlet.http.HttpServletResponse;

public interface AdminStatisticsService {

    OptionsVo options();

    OverviewVo overview(AdminStatisticsQuery query);

    ReservationStatsVo reservations(AdminStatisticsQuery query);

    LabUsageStatsVo labUsage(AdminStatisticsQuery query);

    DeviceStatsVo devices(AdminStatisticsQuery query);

    ConsumableStatsVo consumables(AdminStatisticsQuery query);

    CreditStatsVo credit(AdminStatisticsQuery query);

    void export(AdminStatisticsQuery query, HttpServletResponse response);
}

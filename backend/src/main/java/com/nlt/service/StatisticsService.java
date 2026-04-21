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

    /**
     * 获取概览统计数据
     * @return 处理结果
     */
    Map<String, Long> overview();

    /**
     * 获取管理员首页动态面板数据
     * @return 管理员首页数据
     */
    AdminDashboardVo adminOverview();

    /**
     * 实验室使用情况统计
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> labUsage(LabUsageStatisticsRequest request);

    /**
     * 预约状态统计
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> reservationStatus(ReservationStatusStatisticsRequest request);

    /**
     * 预约类型统计
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> reservationType(ReservationTypeStatisticsRequest request);

    /**
     * 时间分布统计
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> timeDistribution(TimeDistributionStatisticsRequest request);

    /**
     * 违规统计
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> violation(ViolationStatisticsRequest request);

    /**
     * 获取预约趋势统计数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 处理结果
     */
    List<Map<String, Object>> reservationTrend(String startDate, String endDate);

}


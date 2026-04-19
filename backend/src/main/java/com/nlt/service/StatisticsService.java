package com.nlt.service;

import com.nlt.domain.dto.statistics.LabUsageStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationStatusStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationTypeStatisticsRequest;
import com.nlt.domain.dto.statistics.TimeDistributionStatisticsRequest;
import com.nlt.domain.dto.statistics.ViolationStatisticsRequest;
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
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> labUsage(LabUsageStatisticsRequest request);

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> reservationStatus(ReservationStatusStatisticsRequest request);

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> reservationType(ReservationTypeStatisticsRequest request);

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> timeDistribution(TimeDistributionStatisticsRequest request);

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    List<StatisticsItem> violation(ViolationStatisticsRequest request);

    /**
     * 获取预约趋势统计数据
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @return 处理结果
     */
    List<Map<String, Object>> reservationTrend(String startDate, String endDate);

}

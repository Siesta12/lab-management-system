package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.domain.dto.statistics.LabUsageStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationStatusStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationTypeStatisticsRequest;
import com.nlt.domain.dto.statistics.TimeDistributionStatisticsRequest;
import com.nlt.domain.dto.statistics.ViolationStatisticsRequest;
import com.nlt.domain.vo.statistics.StatisticsItem;
import com.nlt.service.StatisticsService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticsService statisticsService;

    /**
     * 获取概览统计数据
     * @return 处理结果
     */
    @GetMapping("/dashboard/overview")
    public ApiResponse<Map<String, Long>> overview() {
        return ApiResponse.success(statisticsService.overview());
    }

    /**
     * 获取预约趋势统计数据
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @return 处理结果
     */
    @GetMapping("/dashboard/reservation-trend")
    public ApiResponse<List<Map<String, Object>>> reservationTrend(@RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
        return ApiResponse.success(statisticsService.reservationTrend(startDate, endDate));
    }

    /**
     * 处理Dashboard相关数据
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/statistics/lab-usage")
    public ApiResponse<List<StatisticsItem>> labUsage(@RequestBody LabUsageStatisticsRequest request) {
        return ApiResponse.success(statisticsService.labUsage(request));
    }

    /**
     * 处理Dashboard相关数据
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/statistics/reservation-status")
    public ApiResponse<List<StatisticsItem>> reservationStatus(@RequestBody ReservationStatusStatisticsRequest request) {
        return ApiResponse.success(statisticsService.reservationStatus(request));
    }

    /**
     * 处理Dashboard相关数据
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/statistics/reservation-type")
    public ApiResponse<List<StatisticsItem>> reservationType(@RequestBody ReservationTypeStatisticsRequest request) {
        return ApiResponse.success(statisticsService.reservationType(request));
    }

    /**
     * 处理Dashboard相关数据
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/statistics/time-distribution")
    public ApiResponse<List<StatisticsItem>> timeDistribution(@RequestBody TimeDistributionStatisticsRequest request) {
        return ApiResponse.success(statisticsService.timeDistribution(request));
    }

    /**
     * 处理Dashboard相关数据
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/statistics/violation")
    public ApiResponse<List<StatisticsItem>> violation(@RequestBody ViolationStatisticsRequest request) {
        return ApiResponse.success(statisticsService.violation(request));
    }

}

package com.nlt.service.impl;

import com.nlt.domain.dto.statistics.LabUsageStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationStatusStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationTypeStatisticsRequest;
import com.nlt.domain.dto.statistics.TimeDistributionStatisticsRequest;
import com.nlt.domain.dto.statistics.ViolationStatisticsRequest;
import com.nlt.domain.vo.statistics.StatisticsItem;
import com.nlt.mapper.ConsumableMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.ViolationMapper;
import com.nlt.service.StatisticsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final LabMapper labMapper;

    private final UserMapper userMapper;

    private final ReservationMapper reservationMapper;

    private final ViolationMapper violationMapper;

    private final ConsumableMapper consumableMapper;

    /**
     * 获取概览统计数据
     * @return 处理结果
     */
    @Override
    public Map<String, Long> overview() {
        return Map.of(
        "totalLabs", labMapper.countPage(null, null, null, null, null, null),
        "totalUsers", userMapper.countPage(null, null, null, null),
        "totalReservations", reservationMapper.countPage(null, null, null, null, null, null),
        "pendingReservations", reservationMapper.countPage(null, null, null, null, 1, null),
        "totalViolations", violationMapper.countPage(null, null, null),
        "lowStockConsumables", (long) consumableMapper.selectWarningList().size()
        );
    }

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<StatisticsItem> labUsage(LabUsageStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByLabUsage(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
        request.getDepartmentId(), request.getLabId()));
    }

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<StatisticsItem> reservationStatus(ReservationStatusStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByStatus(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
        request.getLabId()));
    }

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<StatisticsItem> reservationType(ReservationTypeStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByReservationType(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
        request.getLabId()));
    }

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<StatisticsItem> timeDistribution(TimeDistributionStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByTimeDistribution(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
        request.getLabId()));
    }

    /**
     * 处理统计信息
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<StatisticsItem> violation(ViolationStatisticsRequest request) {
        return toStatisticsItems(violationMapper.countByType(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
        request.getDepartmentId(), request.getViolationType()));
    }

    /**
     * 获取预约趋势统计数据
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @return 处理结果
     */
    @Override
    public List<Map<String, Object>> reservationTrend(String startDate, String endDate) {
        return reservationMapper.reservationTrend(parseDate(startDate), parseDate(endDate));
    }

    /**
     * 转换统计信息
     * @param source 参数
     * @return 数据列表
     */
    private List<StatisticsItem> toStatisticsItems(List<Map<String, Object>> source) {
        long total = source.stream().map(item -> ((Number) item.get("count")).longValue()).reduce(0L, Long::sum);
        return source.stream().map(item -> {
            long count = ((Number) item.get("count")).longValue();
            double rate = total == 0 ? 0D : (double) count / total;
            return new StatisticsItem(String.valueOf(item.get("name")), count, rate);
        }).toList();
    }

    /**
     * 处理统计信息
     * @param value 参数
     * @return 处理结果
     */
    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

}

package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.statistics.AdminStatisticsQuery;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ChartItemVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ConsumableStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.CreditStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.DeviceStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.LabUsageStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionItemVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OverviewVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.RankItemVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ReservationStatsVo;
import com.nlt.service.AdminStatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final int EXPORT_LIMIT = 5000;
    private static final int LOW_CREDIT_SCORE = 60;
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public OptionsVo options() {
        Long departmentId = requireAdminDepartmentId();
        List<OptionItemVo> labs = jdbcTemplate.query("""
                select id, lab_name
                from lab
                where deleted = 0
                  and department_id = :departmentId
                order by lab_name asc, id asc
                """,
            params(departmentId),
            (rs, rowNum) -> new OptionItemVo(rs.getString("lab_name"), String.valueOf(rs.getLong("id"))));

        return new OptionsVo(
            labs,
            List.of(
                new OptionItemVo("待审核", "1"),
                new OptionItemVo("已通过", "2"),
                new OptionItemVo("已驳回", "3"),
                new OptionItemVo("已取消", "4"),
                new OptionItemVo("已完成", "5")
            ),
            List.of(
                new OptionItemVo("课程教学", "1"),
                new OptionItemVo("科研训练", "2"),
                new OptionItemVo("个人预约", "3")
            ),
            List.of(
                new OptionItemVo("预约数据", "reservation"),
                new OptionItemVo("实验报告数据", "experimentReport"),
                new OptionItemVo("耗材统计数据", "consumable"),
                new OptionItemVo("违规/信用数据", "creditViolation")
            )
        );
    }

    @Override
    public OverviewVo overview(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = monthRange(query);
        validateLab(query.getLabId(), departmentId);

        long monthReservationCount = count("""
            select count(distinct r.id)
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labType is null or l.lab_type = :labType)
            """, queryParams(departmentId, range, query));
        long todayReservationCount = count("""
            select count(distinct r.id)
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date = :today
            """, params(departmentId).addValue("today", LocalDate.now()));
        long pendingReservationCount = count("""
            select count(*)
            from lab_reservation r
            join lab l on l.id = r.lab_id
            where l.deleted = 0
              and l.department_id = :departmentId
              and r.status = 1
            """, params(departmentId));
        long brokenDeviceCount = count("""
            select count(*)
            from lab_device d
            join lab l on l.id = d.lab_id
            where d.deleted = 0
              and l.deleted = 0
              and l.department_id = :departmentId
              and d.status != 1
            """, params(departmentId));
        long repairingDeviceCount = count("""
            select count(*)
            from lab_device d
            join lab l on l.id = d.lab_id
            where d.deleted = 0
              and l.deleted = 0
              and l.department_id = :departmentId
              and d.status = 2
            """, params(departmentId));
        long lowStockConsumableCount = count("""
            select count(*)
            from lab_consumable c
            join lab l on l.id = c.lab_id
            where c.deleted = 0
              and l.deleted = 0
              and l.department_id = :departmentId
              and c.stock_quantity <= c.warning_threshold
            """, params(departmentId));
        long monthViolationCount = count("""
            select count(*)
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            """, queryParams(departmentId, range, query));
        BigDecimal averageCreditScore = decimal("""
            select avg(u.credit_score)
            from sys_user u
            where u.deleted = 0
              and u.department_id = :departmentId
            """, params(departmentId));
        long lowCreditUserCount = count("""
            select count(*)
            from sys_user u
            where u.deleted = 0
              and u.department_id = :departmentId
              and u.credit_score < :lowCreditScore
            """, params(departmentId).addValue("lowCreditScore", LOW_CREDIT_SCORE));

        return new OverviewVo(
            monthReservationCount,
            todayReservationCount,
            pendingReservationCount,
            labUsageRate(departmentId, query.getLabId(), query.getLabType(), range.startDate(), range.endDate()),
            brokenDeviceCount,
            repairingDeviceCount,
            lowStockConsumableCount,
            monthViolationCount,
            averageCreditScore,
            lowCreditUserCount
        );
    }

    @Override
    public ReservationStatsVo reservations(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);
        MapSqlParameterSource params = queryParams(departmentId, range, query);

        long totalCount = count("""
            select count(distinct r.id)
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:status is null or r.status = :status)
              and (:reservationType is null or r.reservation_type = :reservationType)
            """, params);
        long pendingCount = count("""
            select count(distinct r.id)
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and r.status = 1
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:reservationType is null or r.reservation_type = :reservationType)
            """, params);
        long completedCount = count("""
            select count(distinct r.id)
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and r.status = 5
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:reservationType is null or r.reservation_type = :reservationType)
            """, params);

        List<ChartItemVo> trend = chart("""
            select date_format(s.reservation_date, '%Y-%m-%d') as name, count(distinct r.id) as value
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and ((:status is not null and r.status = :status) or (:status is null and r.status in (2, 5)))
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:reservationType is null or r.reservation_type = :reservationType)
            group by s.reservation_date
            order by s.reservation_date asc
            """, params);
        List<ChartItemVo> statusDistribution = namedChart("""
            select r.status as name, count(distinct r.id) as value
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:reservationType is null or r.reservation_type = :reservationType)
            group by r.status
            """, params, this::reservationStatusLabel);
        List<ChartItemVo> typeDistribution = namedChart("""
            select r.reservation_type as name, count(distinct r.id) as value
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:status is null or r.status = :status)
            group by r.reservation_type
            """, params, this::reservationTypeLabel);
        List<ChartItemVo> applicantRoleDistribution = chart("""
            select case
                     when exists (
                       select 1 from sys_user_role ur
                       join sys_role role on role.id = ur.role_id
                       where ur.user_id = u.id and role.role_code in ('TEACHER', 'ROLE_TEACHER')
                     ) then '教师'
                     else '学生'
                   end as name,
                   count(distinct s.reservation_id) as value
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join sys_user u on u.id = r.applicant_user_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:status is null or r.status = :status)
              and (:reservationType is null or r.reservation_type = :reservationType)
            group by name
            """, params);
        List<ChartItemVo> labTypeDistribution = chart("""
            select coalesce(nullif(l.lab_type, ''), '未分类') as name,
                   count(distinct r.id) as value
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join lab_reservation_slot s on s.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:status is null or r.status = :status)
              and (:reservationType is null or r.reservation_type = :reservationType)
            group by coalesce(nullif(l.lab_type, ''), '未分类')
            order by value desc, name asc
            """, params);

        return new ReservationStatsVo(
            totalCount,
            pendingCount,
            completedCount,
            totalCount == 0 ? BigDecimal.ZERO : rate(completedCount, totalCount),
            trend,
            statusDistribution,
            typeDistribution,
            applicantRoleDistribution,
            labTypeDistribution
        );
    }

    @Override
    public LabUsageStatsVo labUsage(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);
        MapSqlParameterSource params = queryParams(departmentId, range, query);
        BigDecimal usageRate = labUsageRate(departmentId, query.getLabId(), query.getLabType(), range.startDate(), range.endDate());

        List<RankItemVo> ranking = ranks("""
            select l.id, l.lab_name as name, concat(l.building_name, l.room_no) as secondary,
                   count(distinct s.reservation_id) as value
            from lab l
            left join lab_reservation r on r.lab_id = l.id and r.status in (2, 5)
            left join lab_reservation_slot s on s.reservation_id = r.id
                and s.slot_status = 1
                and s.reservation_date between :startDate and :endDate
            where l.deleted = 0
              and l.department_id = :departmentId
              and (:labId is null or l.id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by l.id, l.lab_name, l.building_name, l.room_no
            order by value desc, l.id asc
            limit 10
            """, params);
        List<RankItemVo> highUsageLabs = new ArrayList<>(ranking);
        List<RankItemVo> idleLabs = ranks("""
            select l.id, l.lab_name as name, concat(l.building_name, l.room_no) as secondary,
                   count(distinct r.id) as value
            from lab l
            left join lab_reservation r on r.lab_id = l.id and r.status in (2, 5)
            left join lab_reservation_slot s on s.reservation_id = r.id
                and s.slot_status = 1
                and s.reservation_date between :startDate and :endDate
            where l.deleted = 0
              and l.department_id = :departmentId
              and (:labId is null or l.id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by l.id, l.lab_name, l.building_name, l.room_no
            order by value asc, l.id asc
            limit 10
            """, params);
        List<RankItemVo> typeUsageRates = labTypeRates(departmentId, query.getLabId(), query.getLabType(), range.startDate(), range.endDate());
        List<ChartItemVo> timeHeat = chart("""
            select p.period_name as name, count(*) as value
            from lab_reservation_slot s
            join lab_reservation r on r.id = s.reservation_id
            join lab l on l.id = r.lab_id
            join class_period p on p.id = s.period_id
            where l.deleted = 0
              and l.department_id = :departmentId
              and r.status in (2, 5)
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by p.id, p.period_name, p.sort_order, p.period_no
            order by p.sort_order asc, p.period_no asc
            """, params);
        long totalOccupiedSlots = timeHeat.stream().mapToLong(ChartItemVo::getValue).sum();
        long highUsageLabCount = count("""
            select count(*) from (
                select l.id, count(distinct s.reservation_id) as reservation_count
                from lab l
                left join lab_reservation r on r.lab_id = l.id and r.status in (2, 5)
                left join lab_reservation_slot s on s.reservation_id = r.id
                    and s.slot_status = 1
                    and s.reservation_date between :startDate and :endDate
                where l.deleted = 0
                  and l.department_id = :departmentId
                  and (:labId is null or l.id = :labId)
                  and (:labType is null or l.lab_type = :labType)
                group by l.id
            ) summary
            where summary.reservation_count > 0
            """, params);
        long idleLabCount = count("""
            select count(*) from (
                select l.id, count(distinct s.reservation_id) as reservation_count
                from lab l
                left join lab_reservation r on r.lab_id = l.id and r.status in (2, 5)
                left join lab_reservation_slot s on s.reservation_id = r.id
                    and s.slot_status = 1
                    and s.reservation_date between :startDate and :endDate
                where l.deleted = 0
                  and l.department_id = :departmentId
                  and (:labId is null or l.id = :labId)
                  and (:labType is null or l.lab_type = :labType)
                group by l.id
            ) summary
            where summary.reservation_count = 0
            """, params);

        return new LabUsageStatsVo(usageRate, totalOccupiedSlots, highUsageLabCount, idleLabCount, ranking, typeUsageRates, highUsageLabs, idleLabs, timeHeat);
    }

    @Override
    public DeviceStatsVo devices(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);
        MapSqlParameterSource params = queryParams(departmentId, range, query);

        long total = count("""
            select count(*) from lab_device d join lab l on l.id = d.lab_id
            where d.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or d.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params);
        long normal = countByDeviceStatus(params, 1);
        long repairing = countByDeviceStatus(params, 2);
        long disabled = countByDeviceStatus(params, 3);
        long repairOrders = count("""
            select count(*) from lab_device_repair rr join lab l on l.id = rr.lab_id
            where l.deleted = 0 and l.department_id = :departmentId
              and rr.created_at >= :startDate and rr.created_at < date_add(:endDate, interval 1 day)
              and (:labId is null or rr.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params);
        List<RankItemVo> labDeviceCounts = ranks("""
            select l.id, l.lab_name as name, concat(l.building_name, l.room_no) as secondary,
                   coalesce(sum(d.quantity), 0) as value
            from lab l
            left join lab_device d on d.lab_id = l.id and d.deleted = 0
            where l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or l.id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by l.id, l.lab_name, l.building_name, l.room_no
            order by value desc, l.id asc
            limit 12
            """, params);
        List<RankItemVo> abnormalDevices = ranks("""
            select d.id, d.device_name as name, concat(l.lab_name, ' / ', d.device_code) as secondary,
                   d.status as value
            from lab_device d
            join lab l on l.id = d.lab_id
            where d.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and d.status != 1
              and (:labId is null or d.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            order by d.updated_at desc, d.id desc
            limit 10
            """, params).stream()
            .map(item -> new RankItemVo(item.getId(), item.getName(), deviceStatusLabel(item.getValue().intValue()) + " / " + item.getSecondary(), item.getValue(), item.getRate()))
            .toList();
        List<ChartItemVo> statusDistribution = withRates(List.of(
            new ChartItemVo("正常", normal, BigDecimal.ZERO),
            new ChartItemVo("维修中", repairing, BigDecimal.ZERO),
            new ChartItemVo("停用", disabled, BigDecimal.ZERO)
        ));
        List<ChartItemVo> categoryDistribution = chart("""
            select coalesce(nullif(d.brand, ''), '未标记品牌') as name,
                   coalesce(sum(d.quantity), 0) as value
            from lab_device d
            join lab l on l.id = d.lab_id
            where d.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or d.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by coalesce(nullif(d.brand, ''), '未标记品牌')
            order by value desc, name asc
            limit 8
            """, params);
        List<ChartItemVo> repairTrend = chart("""
            select date(rr.created_at) as name, count(*) as value
            from lab_device_repair rr
            join lab l on l.id = rr.lab_id
            where l.deleted = 0 and l.department_id = :departmentId
              and rr.created_at >= :startDate and rr.created_at < date_add(:endDate, interval 1 day)
              and (:labId is null or rr.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by date(rr.created_at)
            order by date(rr.created_at) asc
            """, params);

        return new DeviceStatsVo(
            total,
            normal,
            repairing,
            disabled,
            repairOrders,
            "品牌",
            statusDistribution,
            categoryDistribution,
            repairTrend,
            labDeviceCounts,
            abnormalDevices
        );
    }

    @Override
    public ConsumableStatsVo consumables(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = monthRange(query);
        validateLab(query.getLabId(), departmentId);
        MapSqlParameterSource params = queryParams(departmentId, range, query);

        long totalTypeCount = count("""
            select count(*) from lab_consumable c join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params);
        long lowStockCount = count("""
            select count(*) from lab_consumable c join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and c.stock_quantity <= c.warning_threshold
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params);
        long inQuantity = count("""
            select coalesce(sum(abs(log.change_amount)), 0)
            from consumable_stock_log log
            join lab_consumable c on c.id = log.consumable_id
            join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and log.change_type = 'IN'
              and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params);
        long outQuantity = count("""
            select coalesce(sum(abs(log.change_amount)), 0)
            from consumable_stock_log log
            join lab_consumable c on c.id = log.consumable_id
            join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and log.change_type = 'OUT'
              and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params);
        List<RankItemVo> consumptionRanking = ranks("""
            select c.id, c.consumable_name as name, concat(l.lab_name, ' / ', c.unit) as secondary,
                   coalesce(sum(abs(log.change_amount)), 0) as value
            from lab_consumable c
            join lab l on l.id = c.lab_id
            left join consumable_stock_log log on log.consumable_id = c.id
                and log.change_type = 'OUT'
                and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by c.id, c.consumable_name, l.lab_name, c.unit
            order by value desc, c.id asc
            limit 10
            """, params);
        List<RankItemVo> warningList = ranks("""
            select c.id, c.consumable_name as name, concat(l.lab_name, ' / 阈值 ', c.warning_threshold, c.unit) as secondary,
                   c.stock_quantity as value
            from lab_consumable c
            join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and c.stock_quantity <= c.warning_threshold
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            order by c.stock_quantity asc, c.id asc
            limit 10
            """, params);

        List<ChartItemVo> inTrend = chart("""
            select date(log.created_at) as name,
                   coalesce(sum(abs(log.change_amount)), 0) as value
            from consumable_stock_log log
            join lab_consumable c on c.id = log.consumable_id
            join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and log.change_type = 'IN'
              and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by date(log.created_at)
            order by date(log.created_at) asc
            """, params);
        List<ChartItemVo> outTrend = chart("""
            select date(log.created_at) as name,
                   coalesce(sum(abs(log.change_amount)), 0) as value
            from consumable_stock_log log
            join lab_consumable c on c.id = log.consumable_id
            join lab l on l.id = c.lab_id
            where c.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and log.change_type = 'OUT'
              and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by date(log.created_at)
            order by date(log.created_at) asc
            """, params);
        List<RankItemVo> labUsageRanking = ranks("""
            select l.id, l.lab_name as name, coalesce(nullif(l.lab_type, ''), '未分类') as secondary,
                   coalesce(sum(abs(log.change_amount)), 0) as value
            from lab l
            left join lab_consumable c on c.lab_id = l.id and c.deleted = 0
            left join consumable_stock_log log on log.consumable_id = c.id
                and log.change_type = 'OUT'
                and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
            where l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or l.id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by l.id, l.lab_name, l.lab_type
            order by value desc, l.id asc
            limit 10
            """, params);

        return new ConsumableStatsVo(
            totalTypeCount,
            lowStockCount,
            inQuantity,
            outQuantity,
            totalTypeCount == 0 ? BigDecimal.ZERO : rate(lowStockCount, totalTypeCount),
            inTrend,
            outTrend,
            consumptionRanking,
            labUsageRanking,
            warningList
        );
    }

    @Override
    public CreditStatsVo credit(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = monthRange(query);
        MapSqlParameterSource params = queryParams(departmentId, range, query);

        long lateCount = violationCount(params, 2);
        long noShowCount = violationCount(params, 1);
        BigDecimal averageCreditScore = decimal("""
            select avg(credit_score) from sys_user
            where deleted = 0 and department_id = :departmentId
            """, params);
        long lowCreditUserCount = count("""
            select count(*) from sys_user
            where deleted = 0 and department_id = :departmentId and credit_score < :lowCreditScore
            """, params.addValue("lowCreditScore", LOW_CREDIT_SCORE));
        List<ChartItemVo> distribution = namedChart("""
            select v.violation_type as name, count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            group by v.violation_type
            """, params, this::violationTypeLabel);
        List<RankItemVo> lowCreditUsers = ranks("""
            select u.id, u.real_name as name, u.user_no as secondary, u.credit_score as value
            from sys_user u
            where u.deleted = 0 and u.department_id = :departmentId and u.credit_score < :lowCreditScore
            order by u.credit_score asc, u.id asc
            limit 10
            """, params);
        List<RankItemVo> violationRanking = ranks("""
            select u.id, u.real_name as name, u.user_no as secondary, count(v.id) as value
            from sys_user u
            join user_violation_record v on v.user_id = u.id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            group by u.id, u.real_name, u.user_no
            order by value desc, u.id asc
            limit 10
            """, params);
        List<ChartItemVo> violationTrend = chart("""
            select coalesce(slot.first_date, date(r.created_at)) as name, count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            group by coalesce(slot.first_date, date(r.created_at))
            order by coalesce(slot.first_date, date(r.created_at)) asc
            """, params);
        List<ChartItemVo> lateTrend = chart("""
            select coalesce(slot.first_date, date(r.created_at)) as name, count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
              and v.violation_type = 2
            group by coalesce(slot.first_date, date(r.created_at))
            order by coalesce(slot.first_date, date(r.created_at)) asc
            """, params);
        List<ChartItemVo> noShowTrend = chart("""
            select coalesce(slot.first_date, date(r.created_at)) as name, count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
              and v.violation_type = 1
            group by coalesce(slot.first_date, date(r.created_at))
            order by coalesce(slot.first_date, date(r.created_at)) asc
            """, params);
        List<ChartItemVo> creditScoreDistribution = chart("""
            select case
                     when u.credit_score < 60 then '60以下'
                     when u.credit_score < 70 then '60-69'
                     when u.credit_score < 80 then '70-79'
                     when u.credit_score < 90 then '80-89'
                     else '90及以上'
                   end as name,
                   count(*) as value
            from sys_user u
            where u.deleted = 0 and u.department_id = :departmentId
            group by name
            order by value desc, name asc
            """, params);
        List<ChartItemVo> roleViolationDistribution = chart("""
            select case
                     when exists (
                       select 1 from sys_user_role ur
                       join sys_role role on role.id = ur.role_id
                       where ur.user_id = u.id and role.role_code in ('TEACHER', 'ROLE_TEACHER')
                     ) then '教师'
                     else '学生'
                   end as name,
                   count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            group by name
            """, params);
        List<ChartItemVo> reservationTypeDistribution = namedChart("""
            select r.reservation_type as name, count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            group by r.reservation_type
            """, params, this::reservationTypeLabel);
        List<ChartItemVo> timeSegmentDistribution = chart("""
            select case
                     when hour(coalesce(slot.first_start_at, r.created_at)) < 12 then '??'
                     when hour(coalesce(slot.first_start_at, r.created_at)) < 18 then '??'
                     else '??'
                   end as name,
                   count(*) as value
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id,
                       min(timestamp(s.reservation_date, p.start_time)) as first_start_at,
                       min(s.reservation_date) as first_date
                from lab_reservation_slot s
                join class_period p on p.id = s.period_id
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            group by name
            """, params);

        return new CreditStatsVo(
            lateCount,
            noShowCount,
            averageCreditScore,
            lowCreditUserCount,
            distribution,
            violationTrend,
            lateTrend,
            noShowTrend,
            creditScoreDistribution,
            roleViolationDistribution,
            reservationTypeDistribution,
            timeSegmentDistribution,
            lowCreditUsers,
            violationRanking
        );
    }

    @Override
    public void export(AdminStatisticsQuery query, HttpServletResponse response) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);
        String exportType = query.getExportType();
        if (exportType == null || exportType.isBlank()) {
            throw new BusinessException("请选择导出类型");
        }

        ExportData exportData = switch (exportType) {
            case "reservation" -> reservationExport(departmentId, range, query);
            case "experimentReport" -> experimentReportExport(departmentId, range, query);
            case "consumable" -> consumableExport(departmentId, range, query);
            case "creditViolation" -> creditViolationExport(departmentId, range, query);
            default -> throw new BusinessException("不支持的导出类型");
        };
        if (exportData.rows().size() > EXPORT_LIMIT) {
            throw new BusinessException("单次导出最多支持 " + EXPORT_LIMIT + " 条，请缩小筛选范围");
        }

        String filename = "统计分析-" + exportData.title() + "-" + LocalDate.now().format(FILE_DATE_FORMATTER) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            writeSheet(workbook, exportData);
            workbook.write(response.getOutputStream());
            workbook.dispose();
        } catch (IOException ex) {
            throw new BusinessException("导出 Excel 失败");
        }
    }

    private ExportData reservationExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        MapSqlParameterSource params = queryParams(departmentId, range, query).addValue("limit", EXPORT_LIMIT + 1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select r.reservation_no as reservationNo,
                   l.lab_name as labName,
                   u.real_name as applicantName,
                   case when exists (
                       select 1 from sys_user_role ur join sys_role role on role.id = ur.role_id
                       where ur.user_id = u.id and role.role_code in ('TEACHER', 'ROLE_TEACHER')
                   ) then '教师' else '学生' end as applicantRole,
                   r.reservation_type as reservationType,
                   r.status as status,
                   min(s.reservation_date) as reservationDate,
                   concat(min(date_format(p.start_time, '%H:%i')), ' - ', max(date_format(p.end_time, '%H:%i'))) as timeRange,
                   r.created_at as createdAt,
                   audit.created_at as auditedAt
            from lab_reservation r
            join lab l on l.id = r.lab_id
            join sys_user u on u.id = r.applicant_user_id
            join lab_reservation_slot s on s.reservation_id = r.id
            join class_period p on p.id = s.period_id
            left join (
                select reservation_id, max(created_at) as created_at
                from reservation_audit_log
                where audit_action in (2, 3)
                group by reservation_id
            ) audit on audit.reservation_id = r.id
            where l.deleted = 0
              and l.department_id = :departmentId
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and (:status is null or r.status = :status)
              and (:reservationType is null or r.reservation_type = :reservationType)
            group by r.id, r.reservation_no, l.lab_name, u.real_name, applicantRole, r.reservation_type, r.status, r.created_at, audit.created_at
            order by min(s.reservation_date) desc, r.id desc
            limit :limit
            """, params);
        List<List<Object>> data = rows.stream()
            .map(row -> List.of(
                value(row, "reservationNo"),
                value(row, "labName"),
                value(row, "applicantName"),
                value(row, "applicantRole"),
                reservationTypeLabel(row.get("reservationType")),
                reservationStatusLabel(row.get("status")),
                value(row, "reservationDate"),
                value(row, "timeRange"),
                value(row, "createdAt"),
                value(row, "auditedAt")
            ))
            .toList();
        return new ExportData("预约数据", List.of("预约编号", "实验室", "申请人", "角色", "预约类型", "状态", "预约日期", "时间段", "创建时间", "审核时间"), data);
    }

    private ExportData experimentReportExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        MapSqlParameterSource params = queryParams(departmentId, range, query).addValue("limit", EXPORT_LIMIT + 1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select r.report_no as reportNo,
                   lr.reservation_no as reservationNo,
                   l.lab_name as labName,
                   stu.real_name as studentName,
                   r.status as status,
                   r.submitted_at as submittedAt,
                   tea.real_name as teacherName,
                   r.reviewed_at as reviewedAt
            from lab_experiment_report r
            left join lab_reservation lr on lr.id = r.reservation_id
            join lab l on l.id = r.lab_id
            join sys_user stu on stu.id = r.student_id
            join sys_user tea on tea.id = r.teacher_id
            where r.deleted = 0
              and r.department_id = :departmentId
              and r.experiment_date between :startDate and :endDate
              and (:labId is null or r.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            order by r.updated_at desc, r.id desc
            limit :limit
            """, params);
        List<List<Object>> data = rows.stream()
            .map(row -> List.of(
                value(row, "reportNo"),
                value(row, "reservationNo"),
                value(row, "labName"),
                value(row, "studentName"),
                reportStatusLabel(row.get("status")),
                value(row, "submittedAt"),
                value(row, "teacherName"),
                value(row, "reviewedAt")
            ))
            .toList();
        return new ExportData("实验报告数据", List.of("报告编号", "关联预约", "实验室", "提交人", "状态", "提交时间", "审核人", "审核时间"), data);
    }

    private ExportData consumableExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        MapSqlParameterSource params = queryParams(departmentId, range, query).addValue("limit", EXPORT_LIMIT + 1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select c.consumable_name as consumableName,
                   l.lab_name as labName,
                   c.specification as specification,
                   c.stock_quantity as stockQuantity,
                   c.warning_threshold as warningThreshold,
                   coalesce(sum(case when log.change_type = 'IN' then abs(log.change_amount) else 0 end), 0) as inQuantity,
                   coalesce(sum(case when log.change_type = 'OUT' then abs(log.change_amount) else 0 end), 0) as outQuantity,
                   case when c.stock_quantity <= c.warning_threshold then '库存预警' else '正常' end as stockStatus
            from lab_consumable c
            join lab l on l.id = c.lab_id
            left join consumable_stock_log log on log.consumable_id = c.id
                and log.created_at >= :startDate and log.created_at < date_add(:endDate, interval 1 day)
            where c.deleted = 0
              and l.deleted = 0
              and l.department_id = :departmentId
              and (:labId is null or c.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            group by c.id, c.consumable_name, l.lab_name, c.specification, c.stock_quantity, c.warning_threshold
            order by c.id desc
            limit :limit
            """, params);
        List<List<Object>> data = rows.stream()
            .map(row -> List.of(
                value(row, "consumableName"),
                value(row, "labName"),
                value(row, "specification"),
                value(row, "stockQuantity"),
                value(row, "warningThreshold"),
                value(row, "inQuantity"),
                value(row, "outQuantity"),
                value(row, "stockStatus")
            ))
            .toList();
        return new ExportData("耗材统计数据", List.of("耗材名称", "实验室", "规格", "当前库存", "预警阈值", "本月入库", "本月出库", "库存状态"), data);
    }

    private ExportData creditViolationExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        MapSqlParameterSource params = queryParams(departmentId, range, query).addValue("limit", EXPORT_LIMIT + 1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            select u.real_name as realName,
                   u.user_no as userNo,
                   case when exists (
                       select 1 from sys_user_role ur join sys_role role on role.id = ur.role_id
                       where ur.user_id = u.id and role.role_code in ('TEACHER', 'ROLE_TEACHER')
                   ) then '??' else '??' end as userRole,
                   v.violation_type as violationType,
                   v.score_change as scoreChange,
                   coalesce(slot.first_date, date(r.created_at)) as createdAt,
                   u.credit_score as creditScore,
                   v.remark as remark
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
            order by coalesce(slot.first_date, date(r.created_at)) desc, v.id desc
            limit :limit
            """, params);
        List<List<Object>> data = rows.stream()
            .map(row -> List.of(
                value(row, "realName"),
                value(row, "userNo"),
                value(row, "userRole"),
                violationTypeLabel(row.get("violationType")),
                value(row, "scoreChange"),
                value(row, "createdAt"),
                value(row, "creditScore"),
                value(row, "remark")
            ))
            .toList();
        return new ExportData("??????", List.of("????", "???", "??", "????", "??", "????", "?????", "??"), data);
    }

    private void writeSheet(SXSSFWorkbook workbook, ExportData exportData) {
        Sheet sheet = workbook.createSheet(exportData.title());
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        Row header = sheet.createRow(0);
        for (int i = 0; i < exportData.headers().size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(exportData.headers().get(i));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4200);
        }
        for (int rowIndex = 0; rowIndex < exportData.rows().size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<Object> values = exportData.rows().get(rowIndex);
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                row.createCell(columnIndex).setCellValue(Objects.toString(values.get(columnIndex), ""));
            }
        }
    }

    private Long requireAdminDepartmentId() {
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        if (departmentId == null) {
            throw new BusinessException(403, "仅管理员可访问统计分析");
        }
        return departmentId;
    }

    private void validateLab(Long labId, Long departmentId) {
        if (labId == null) {
            return;
        }
        long exists = count("""
            select count(*) from lab
            where id = :labId and department_id = :departmentId and deleted = 0
            """, params(departmentId).addValue("labId", labId));
        if (exists == 0) {
            throw new BusinessException(403, "无权访问该实验室数据");
        }
    }

    private DateRange defaultRange(AdminStatisticsQuery query) {
        LocalDate endDate = query.getEndDate() == null ? LocalDate.now() : query.getEndDate();
        LocalDate startDate = query.getStartDate() == null ? endDate.minusDays(29) : query.getStartDate();
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        return new DateRange(startDate, endDate);
    }

    private DateRange monthRange(AdminStatisticsQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate startDate = query.getStartDate() == null ? now.withDayOfMonth(1) : query.getStartDate();
        LocalDate endDate = query.getEndDate() == null ? now : query.getEndDate();
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        return new DateRange(startDate, endDate);
    }

    private MapSqlParameterSource params(Long departmentId) {
        return new MapSqlParameterSource().addValue("departmentId", departmentId);
    }

    private MapSqlParameterSource queryParams(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        return params(departmentId)
            .addValue("startDate", range.startDate())
            .addValue("endDate", range.endDate())
            .addValue("labId", query.getLabId())
            .addValue("labType", trimToNull(query.getLabType()))
            .addValue("status", query.getStatus())
            .addValue("reservationType", query.getReservationType());
    }

    private long count(String sql, MapSqlParameterSource params) {
        Number value = jdbcTemplate.queryForObject(sql, params, Number.class);
        return value == null ? 0L : value.longValue();
    }

    private BigDecimal decimal(String sql, MapSqlParameterSource params) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private long countByDeviceStatus(MapSqlParameterSource params, int status) {
        return count("""
            select count(*) from lab_device d join lab l on l.id = d.lab_id
            where d.deleted = 0 and l.deleted = 0 and l.department_id = :departmentId
              and (:labId is null or d.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
              and d.status = :deviceStatus
            """, new MapSqlParameterSource(params.getValues()).addValue("deviceStatus", status));
    }

    private long violationCount(MapSqlParameterSource params, int violationType) {
        return count("""
            select count(*)
            from user_violation_record v
            join sys_user u on u.id = v.user_id
            left join lab_reservation r on r.id = v.reservation_id
            left join (
                select s.reservation_id, min(s.reservation_date) as first_date
                from lab_reservation_slot s
                where s.slot_status = 1
                group by s.reservation_id
            ) slot on slot.reservation_id = v.reservation_id
            where u.deleted = 0
              and u.department_id = :departmentId
              and coalesce(slot.first_date, date(r.created_at)) between :startDate and :endDate
              and v.violation_type = :violationType
            """, new MapSqlParameterSource(params.getValues()).addValue("violationType", violationType));
    }

    private List<ChartItemVo> chart(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        return withRates(rows.stream()
            .map(row -> new ChartItemVo(Objects.toString(row.get("name"), ""), number(row.get("value")), BigDecimal.ZERO))
            .toList());
    }

    private List<ChartItemVo> namedChart(String sql, MapSqlParameterSource params, LabelResolver resolver) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        return withRates(rows.stream()
            .map(row -> new ChartItemVo(resolver.resolve(row.get("name")), number(row.get("value")), BigDecimal.ZERO))
            .toList());
    }

    private List<ChartItemVo> withRates(List<ChartItemVo> items) {
        long total = items.stream().mapToLong(ChartItemVo::getValue).sum();
        if (total == 0) {
            return items;
        }
        return items.stream()
            .map(item -> new ChartItemVo(item.getName(), item.getValue(), rate(item.getValue(), total)))
            .toList();
    }

    private List<RankItemVo> ranks(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        long total = rows.stream().mapToLong(row -> number(row.get("value"))).sum();
        return rows.stream()
            .map(row -> new RankItemVo(
                number(row.get("id")),
                Objects.toString(row.get("name"), ""),
                Objects.toString(row.get("secondary"), ""),
                number(row.get("value")),
                total == 0 ? BigDecimal.ZERO : rate(number(row.get("value")), total)
            ))
            .toList();
    }

    private BigDecimal labUsageRate(Long departmentId, Long labId, String labType, LocalDate startDate, LocalDate endDate) {
        long occupied = count("""
            select count(*)
            from lab_reservation_slot s
            join lab_reservation r on r.id = s.reservation_id
            join lab l on l.id = s.lab_id
            where l.deleted = 0
              and l.department_id = :departmentId
              and r.status in (2, 5)
              and s.slot_status = 1
              and s.reservation_date between :startDate and :endDate
              and (:labId is null or s.lab_id = :labId)
              and (:labType is null or l.lab_type = :labType)
            """, params(departmentId).addValue("labId", labId).addValue("labType", trimToNull(labType)).addValue("startDate", startDate).addValue("endDate", endDate));
        long totalOpenSlots = totalOpenSlots(departmentId, labId, startDate, endDate, trimToNull(labType));
        return totalOpenSlots == 0 ? BigDecimal.ZERO : rate(occupied, totalOpenSlots);
    }

    private List<RankItemVo> labTypeRates(Long departmentId, Long labId, String selectedLabType, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> labTypes = jdbcTemplate.queryForList("""
            select coalesce(nullif(lab_type, ''), '未分类') as labType
            from lab
            where deleted = 0
              and department_id = :departmentId
              and (:labId is null or id = :labId)
              and (:selectedLabType is null or lab_type = :selectedLabType)
            group by coalesce(nullif(lab_type, ''), '未分类')
            order by labType asc
            """, params(departmentId).addValue("labId", labId).addValue("selectedLabType", trimToNull(selectedLabType)));
        List<RankItemVo> result = new ArrayList<>();
        for (Map<String, Object> row : labTypes) {
            String labType = Objects.toString(row.get("labType"), "未分类");
            MapSqlParameterSource p = params(departmentId)
                .addValue("labId", labId)
                .addValue("labType", labType)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
            long occupied = count("""
                select count(*)
                from lab_reservation_slot s
                join lab_reservation r on r.id = s.reservation_id
                join lab l on l.id = s.lab_id
                where l.deleted = 0
                  and l.department_id = :departmentId
                  and coalesce(nullif(l.lab_type, ''), '未分类') = :labType
                  and r.status in (2, 5)
                  and s.slot_status = 1
                  and s.reservation_date between :startDate and :endDate
                  and (:labId is null or s.lab_id = :labId)
                """, p);
            long total = totalOpenSlots(departmentId, labId, startDate, endDate, labType);
            result.add(new RankItemVo(null, labType, "预约使用率", occupied, total == 0 ? BigDecimal.ZERO : rate(occupied, total)));
        }
        return result.stream()
            .sorted((left, right) -> right.getRate().compareTo(left.getRate()))
            .toList();
    }

    private long totalOpenSlots(Long departmentId, Long labId, LocalDate startDate, LocalDate endDate, String labType) {
        MapSqlParameterSource p = params(departmentId).addValue("labId", labId).addValue("labType", labType);
        Map<Integer, Long> openSlotsByWeekday = new LinkedHashMap<>();
        jdbcTemplate.query("""
            select os.weekday as weekday, count(*) as value
            from lab_open_slot os
            join lab l on l.id = os.lab_id
            where l.deleted = 0
              and l.department_id = :departmentId
              and os.status = 1
              and (:labId is null or os.lab_id = :labId)
              and (:labType is null or coalesce(nullif(l.lab_type, ''), '未分类') = :labType)
            group by os.weekday
            """, p, (RowCallbackHandler) rs -> openSlotsByWeekday.put(rs.getInt("weekday"), rs.getLong("value")));
        long total = 0L;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            total += openSlotsByWeekday.getOrDefault(date.getDayOfWeek().getValue(), 0L);
        }
        if (total > 0) {
            return total;
        }
        long labCount = count("""
            select count(*) from lab
            where deleted = 0
              and department_id = :departmentId
              and (:labId is null or id = :labId)
              and (:labType is null or coalesce(nullif(lab_type, ''), '未分类') = :labType)
            """, p);
        long periodCount = count("select count(*) from class_period where status = 1", new MapSqlParameterSource());
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        return labCount * periodCount * Math.max(days, 0L);
    }

    private BigDecimal rate(long value, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private Object value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return value == null ? "" : value;
    }

    private String reservationStatusLabel(Object value) {
        int status = (int) number(value);
        return switch (status) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            case 4 -> "已取消";
            case 5 -> "已完成";
            default -> "未知状态";
        };
    }

    private String reservationTypeLabel(Object value) {
        int type = (int) number(value);
        return switch (type) {
            case 1 -> "课程教学";
            case 2 -> "科研训练";
            case 3 -> "个人预约";
            default -> "未分类";
        };
    }

    private String deviceStatusLabel(int status) {
        return switch (status) {
            case 1 -> "正常";
            case 2 -> "维修中";
            case 3 -> "停用";
            default -> "异常";
        };
    }

    private String reportStatusLabel(Object value) {
        int status = (int) number(value);
        return switch (status) {
            case 1 -> "草稿";
            case 2 -> "待审核";
            case 3 -> "已通过";
            case 4 -> "已退回";
            default -> "未知状态";
        };
    }

    private String violationTypeLabel(Object value) {
        int type = (int) number(value);
        return switch (type) {
            case 1 -> "爽约";
            case 2 -> "迟到";
            case 3 -> "违规使用";
            case 4 -> "其他";
            default -> "未分类";
        };
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    private record ExportData(String title, List<String> headers, List<List<Object>> rows) {
    }

    @FunctionalInterface
    private interface LabelResolver {
        String resolve(Object value);
    }
}

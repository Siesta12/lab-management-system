package com.nlt.service.impl;

import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.statistics.LabUsageStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationStatusStatisticsRequest;
import com.nlt.domain.dto.statistics.ReservationTypeStatisticsRequest;
import com.nlt.domain.dto.statistics.TimeDistributionStatisticsRequest;
import com.nlt.domain.dto.statistics.ViolationStatisticsRequest;
import com.nlt.domain.entity.ClassPeriodEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.LabMaintenanceEntity;
import com.nlt.domain.entity.LabOpenSlotEntity;
import com.nlt.domain.vo.dashboard.AdminDashboardVo;
import com.nlt.domain.vo.dashboard.DashboardCardVo;
import com.nlt.domain.vo.dashboard.DashboardLabOccupancyVo;
import com.nlt.domain.vo.dashboard.DashboardReservationItemVo;
import com.nlt.domain.vo.dashboard.DashboardSectionItemVo;
import com.nlt.domain.vo.dashboard.DashboardSectionVo;
import com.nlt.domain.vo.dashboard.TodayReservationSummaryVo;
import com.nlt.domain.vo.statistics.StatisticsItem;
import com.nlt.mapper.ClassPeriodMapper;
import com.nlt.mapper.ConsumableMapper;
import com.nlt.mapper.DeviceMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.LabMaintenanceMapper;
import com.nlt.mapper.LabOpenSlotMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.ViolationMapper;
import com.nlt.service.StatisticsService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LabMapper labMapper;
    private final LabOpenSlotMapper labOpenSlotMapper;
    private final ClassPeriodMapper classPeriodMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final ViolationMapper violationMapper;
    private final ConsumableMapper consumableMapper;
    private final DeviceMapper deviceMapper;
    private final LabMaintenanceMapper labMaintenanceMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public Map<String, Long> overview() {
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        return Map.of(
            "totalLabs", labMapper.countPage(null, null, null, null, departmentId, null, null),
            "totalUsers", userMapper.countPage(null, null, departmentId, null, null),
            "totalReservations", reservationMapper.countPage(null, null, null, null, null, null, departmentId, false),
            "pendingReservations", reservationMapper.countPage(null, null, null, null, 1, null, departmentId, false),
            "totalViolations", violationMapper.countPage(null, null, null),
            "lowStockConsumables", (long) consumableMapper.selectWarningList(departmentId).size()
        );
    }

    @Override
    public AdminDashboardVo adminOverview() {
        LocalDate today = LocalDate.now();
        int weekday = today.getDayOfWeek().getValue();
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();

        List<LabEntity> labs = labMapper.selectPage(0, 1000, null, null, null, null, departmentId, 1, 1).stream()
            .filter(lab -> !Objects.equals(lab.getDeleted(), 1))
            .filter(lab -> Objects.equals(lab.getOpenStatus(), 1))
            .filter(lab -> Objects.equals(lab.getLabStatus(), 1))
            .toList();
        List<ClassPeriodEntity> periods = classPeriodMapper.selectActiveList();
        List<Long> labIds = labs.stream().map(LabEntity::getId).toList();
        List<Long> periodIds = periods.stream().map(ClassPeriodEntity::getId).toList();

        List<DashboardReservationItemVo> todaySlotRows = reservationMapper.selectDashboardDailySlots(today, departmentId);
        Set<Long> visibleLabIds = labs.stream().map(LabEntity::getId).collect(Collectors.toSet());
        List<LabMaintenanceEntity> todayMaintenanceRows = labMaintenanceMapper.selectActiveByDate(today).stream()
            .filter(item -> visibleLabIds.contains(item.getLabId()))
            .toList();
        Map<Long, LabEntity> labMap = labs.stream().collect(Collectors.toMap(LabEntity::getId, lab -> lab));

        Map<Long, List<DashboardReservationItemVo>> reservationGroups = todaySlotRows.stream()
            .collect(Collectors.groupingBy(DashboardReservationItemVo::getReservationId, HashMap::new, Collectors.toList()));
        Map<String, List<DashboardReservationItemVo>> slotGroups = todaySlotRows.stream()
            .collect(Collectors.groupingBy(item -> slotKey(item.getLabId(), item.getReservationDate(), item.getPeriodId())));

        List<DashboardReservationItemVo> todayReservations = reservationGroups.values().stream()
            .map(items -> buildReservationItem(items, slotGroups, todayMaintenanceRows))
            .sorted(Comparator
                .comparing(DashboardReservationItemVo::getTimeRange, Comparator.nullsLast(String::compareTo))
                .thenComparing(DashboardReservationItemVo::getPriorityLevel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(DashboardReservationItemVo::getReservationId))
            .toList();

        List<DashboardReservationItemVo> pendingReservations = todayReservations.stream()
            .filter(item -> Objects.equals(item.getStatus(), 1))
            .limit(5)
            .toList();

        long totalReservations = todayReservations.size();
        long pendingCount = todayReservations.stream().filter(item -> Objects.equals(item.getStatus(), 1)).count();
        Set<Long> conflictReservationIds = new HashSet<>();
        for (List<DashboardReservationItemVo> slotItems : slotGroups.values()) {
            Set<Long> reservationIds = slotItems.stream()
                .map(DashboardReservationItemVo::getReservationId)
                .collect(Collectors.toSet());
            if (reservationIds.size() > 1) {
                conflictReservationIds.addAll(reservationIds);
            }
        }
        Set<String> maintenanceSlotKeys = todayMaintenanceRows.stream()
            .filter(item -> Objects.equals(item.getStatus(), 1))
            .map(item -> slotKey(item.getLabId(), item.getMaintenanceDate().toString(), item.getPeriodId()))
            .collect(Collectors.toSet());
        conflictReservationIds.addAll(todayReservations.stream()
            .filter(item -> maintenanceSlotKeys.contains(slotKey(item.getLabId(), item.getReservationDate(), item.getPeriodId())))
            .map(DashboardReservationItemVo::getReservationId)
            .collect(Collectors.toSet()));
        long conflictCount = todayReservations.stream()
            .filter(item -> conflictReservationIds.contains(item.getReservationId()))
            .count();
        long upcomingCount = todayReservations.stream().filter(item -> isUpcoming(item.getStartTime())).count();

        long period12 = countByPeriodRange(todayReservations, 1, 2);
        long period34 = countByPeriodRange(todayReservations, 3, 4);
        long period56 = countByPeriodRange(todayReservations, 5, 6);

        long openLabsCount = labs.size();
        long lowStockCount = consumableMapper.selectWarningList(departmentId).size();
        long deviceMaintenanceCount = deviceMapper.selectWarningList(departmentId).size();
        long pendingAuditCount = reservationMapper.countPendingAudit(departmentId);
        List<DashboardCardVo> cards = List.of(
            new DashboardCardVo("开放实验室", String.valueOf(openLabsCount), "当前可预约实验室", "success"),
            new DashboardCardVo("待审核申请", String.valueOf(pendingAuditCount), "待管理员审核", "warning"),
            new DashboardCardVo("低库存耗材", String.valueOf(lowStockCount), "需要补货提醒", "accent"),
            new DashboardCardVo("设备维护中", String.valueOf(deviceMaintenanceCount), "需要关注设备状态", "brand")
        );

        TodayReservationSummaryVo todayOverview = new TodayReservationSummaryVo(
            totalReservations,
            period12,
            period34,
            period56,
            pendingCount,
            conflictCount,
            upcomingCount
        );

        List<DashboardSectionVo> pendingSections = buildPendingSections(labMap, pendingReservations, departmentId);
        List<DashboardLabOccupancyVo> occupancyRates = buildOccupancyRates(labs, labIds, periodIds, weekday,
            todaySlotRows, todayMaintenanceRows);

        return new AdminDashboardVo(cards, todayOverview, todayReservations, pendingSections, occupancyRates, pendingReservations);
    }

    @Override
    public List<StatisticsItem> labUsage(LabUsageStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByLabUsage(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
            request.getDepartmentId(), request.getLabId()));
    }

    @Override
    public List<StatisticsItem> reservationStatus(ReservationStatusStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByStatus(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
            request.getLabId()));
    }

    @Override
    public List<StatisticsItem> reservationType(ReservationTypeStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByReservationType(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
            request.getLabId()));
    }

    @Override
    public List<StatisticsItem> timeDistribution(TimeDistributionStatisticsRequest request) {
        return toStatisticsItems(reservationMapper.countByTimeDistribution(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
            request.getLabId()));
    }

    @Override
    public List<StatisticsItem> violation(ViolationStatisticsRequest request) {
        return toStatisticsItems(violationMapper.countByType(parseDate(request.getStartDate()), parseDate(request.getEndDate()),
            request.getDepartmentId(), request.getViolationType()));
    }

    @Override
    public List<Map<String, Object>> reservationTrend(String startDate, String endDate) {
        return reservationMapper.reservationTrend(parseDate(startDate), parseDate(endDate));
    }

    private List<StatisticsItem> toStatisticsItems(List<Map<String, Object>> source) {
        long total = source.stream().map(item -> ((Number) item.get("count")).longValue()).reduce(0L, Long::sum);
        return source.stream().map(item -> {
            long count = ((Number) item.get("count")).longValue();
            double rate = total == 0 ? 0D : (double) count / total;
            return new StatisticsItem(String.valueOf(item.get("name")), count, rate);
        }).toList();
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private List<DashboardSectionVo> buildPendingSections(Map<Long, LabEntity> labMap,
        List<DashboardReservationItemVo> pendingReservations,
        Long departmentId) {
        List<DashboardSectionItemVo> approvalItems = pendingReservations.stream()
            .map(item -> new DashboardSectionItemVo(
                item.getLabName(),
                item.getApplicantName() + " · " + item.getTimeRange(),
                item.getTypeLabel(),
                item.getStatusLabel(),
                "warning"
            ))
            .toList();

        List<DashboardSectionItemVo> stockItems = consumableMapper.selectWarningList(departmentId).stream()
            .limit(5)
            .map(item -> new DashboardSectionItemVo(
                item.getConsumableName(),
                lookupLabName(labMap, item.getLabId()) + " · 库存偏低",
                "库存预警",
                "待补货",
                "accent"
            ))
            .toList();

        List<DashboardSectionItemVo> deviceItems = deviceMapper.selectWarningList(departmentId).stream()
            .limit(5)
            .map(item -> new DashboardSectionItemVo(
                item.getDeviceName(),
                lookupLabName(labMap, item.getLabId()) + " · " + resolveDeviceStatus(item.getStatus()),
                "设备状态",
                "设备关注",
                "brand"
            ))
            .toList();

        return List.of(
            new DashboardSectionVo("待审核", "审核队列", approvalItems),
            new DashboardSectionVo("库存预警", "补货提醒", stockItems),
            new DashboardSectionVo("设备异常", "设备维护", deviceItems)
        );
    }

    private List<DashboardLabOccupancyVo> buildOccupancyRates(List<LabEntity> labs, List<Long> labIds,
        List<Long> periodIds, int weekday, List<DashboardReservationItemVo> todaySlotRows, List<LabMaintenanceEntity> maintenanceRows) {
        if (labIds.isEmpty() || periodIds.isEmpty()) {
            return List.of();
        }

        List<LabOpenSlotEntity> openSlotRows = labOpenSlotMapper.selectActiveByLabIdsAndWeekdayAndPeriods(labIds, weekday, periodIds);
        Map<Long, Set<Long>> openPeriodsByLab = new HashMap<>();
        for (LabOpenSlotEntity row : openSlotRows) {
            if (!Objects.equals(row.getStatus(), 1)) {
                continue;
            }
            openPeriodsByLab.computeIfAbsent(row.getLabId(), key -> new HashSet<>()).add(row.getPeriodId());
        }

        Map<Long, Set<Long>> maintenancePeriodsByLab = new HashMap<>();
        for (LabMaintenanceEntity maintenance : maintenanceRows) {
            if (!Objects.equals(maintenance.getStatus(), 1)) {
                continue;
            }
            maintenancePeriodsByLab.computeIfAbsent(maintenance.getLabId(), key -> new HashSet<>()).add(maintenance.getPeriodId());
        }

        Map<Long, Set<Long>> occupiedPeriodsByLab = new HashMap<>();
        for (DashboardReservationItemVo row : todaySlotRows) {
            if (!Objects.equals(row.getStatus(), 2) && !Objects.equals(row.getStatus(), 5)) {
                continue;
            }
            Set<Long> openPeriods = openPeriodsByLab.get(row.getLabId());
            if (openPeriods == null || !openPeriods.contains(row.getPeriodId())) {
                continue;
            }
            Set<Long> maintenancePeriods = maintenancePeriodsByLab.getOrDefault(row.getLabId(), Set.of());
            if (maintenancePeriods.contains(row.getPeriodId())) {
                continue;
            }
            occupiedPeriodsByLab.computeIfAbsent(row.getLabId(), key -> new HashSet<>()).add(row.getPeriodId());
        }

        Map<String, OccupancyAggregate> aggregateMap = new HashMap<>();
        for (LabEntity lab : labs) {
            Set<Long> openPeriods = openPeriodsByLab.getOrDefault(lab.getId(), Set.of());
            Set<Long> maintenancePeriods = maintenancePeriodsByLab.getOrDefault(lab.getId(), Set.of());
            int totalOpenSlots = (int) openPeriods.stream()
                .filter(periodId -> !maintenancePeriods.contains(periodId))
                .count();
            if (totalOpenSlots <= 0) {
                continue;
            }

            int occupiedSlots = occupiedPeriodsByLab.getOrDefault(lab.getId(), Set.of()).size();
            String labType = lab.getLabType() == null || lab.getLabType().isBlank() ? "未分类实验室" : lab.getLabType();
            OccupancyAggregate aggregate = aggregateMap.computeIfAbsent(labType, key -> new OccupancyAggregate());
            aggregate.occupiedSlots += occupiedSlots;
            aggregate.totalOpenSlots += totalOpenSlots;
            aggregate.labCount += 1;
        }

        return aggregateMap.entrySet().stream()
            .map(entry -> {
                OccupancyAggregate aggregate = entry.getValue();
                double rate = aggregate.totalOpenSlots == 0 ? 0D : (double) aggregate.occupiedSlots / aggregate.totalOpenSlots;
                return new DashboardLabOccupancyVo(entry.getKey(), aggregate.occupiedSlots, aggregate.totalOpenSlots, rate, aggregate.labCount);
            })
            .sorted(Comparator.comparing(DashboardLabOccupancyVo::getOccupancyRate, Comparator.reverseOrder())
                .thenComparing(DashboardLabOccupancyVo::getTotalOpenSlots, Comparator.reverseOrder())
                .thenComparing(DashboardLabOccupancyVo::getLabType))
            .toList();
    }

    private DashboardReservationItemVo buildReservationItem(List<DashboardReservationItemVo> rows,
        Map<String, List<DashboardReservationItemVo>> slotGroups,
        List<LabMaintenanceEntity> maintenanceRows) {
        DashboardReservationItemVo first = rows.stream()
            .sorted(Comparator.comparing(DashboardReservationItemVo::getStartTime, Comparator.nullsLast(String::compareTo))
                .thenComparing(DashboardReservationItemVo::getPeriodId, Comparator.nullsLast(Long::compareTo)))
            .findFirst()
            .orElse(new DashboardReservationItemVo());
        DashboardReservationItemVo last = rows.stream()
            .sorted(Comparator.comparing(DashboardReservationItemVo::getEndTime, Comparator.nullsLast(String::compareTo))
                .thenComparing(DashboardReservationItemVo::getPeriodId, Comparator.nullsLast(Long::compareTo)))
            .reduce((left, right) -> right)
            .orElse(first);

        DashboardReservationItemVo item = new DashboardReservationItemVo();
        item.setReservationId(first.getReservationId());
        item.setReservationNo(first.getReservationNo());
        item.setLabId(first.getLabId());
        item.setLabName(first.getLabName());
        item.setApplicantUserId(first.getApplicantUserId());
        item.setApplicantName(first.getApplicantName());
        item.setApplicantCreditScore(first.getApplicantCreditScore());
        item.setReservationType(first.getReservationType());
        item.setPriorityLevel(first.getPriorityLevel());
        item.setStatus(first.getStatus());
        item.setReservationDate(first.getReservationDate());
        item.setCreatedAt(first.getCreatedAt());
        item.setTimeRange(joinTimeRange(first.getStartTime(), last.getEndTime()));
        item.setTypeLabel(resolveReservationTypeLabel(first.getReservationType()));
        item.setStatusLabel(resolveStatusLabel(first.getStatus()));
        item.setNote(buildReservationNote(rows, slotGroups, maintenanceRows, first));
        return item;
    }

    private String buildReservationNote(List<DashboardReservationItemVo> rows,
        Map<String, List<DashboardReservationItemVo>> slotGroups,
        List<LabMaintenanceEntity> maintenanceRows,
        DashboardReservationItemVo first) {
        boolean hasConflict = rows.stream().anyMatch(row -> {
            List<DashboardReservationItemVo> items = slotGroups.get(slotKey(row.getLabId(), row.getReservationDate(), row.getPeriodId()));
            return items != null && items.stream().map(DashboardReservationItemVo::getReservationId).distinct().count() > 1;
        });
        boolean hasMaintenance = rows.stream().anyMatch(row -> maintenanceRows.stream().anyMatch(maintenance ->
            Objects.equals(maintenance.getLabId(), row.getLabId())
                && Objects.equals(maintenance.getMaintenanceDate().toString(), row.getReservationDate())
                && Objects.equals(maintenance.getPeriodId(), row.getPeriodId())));

        if (Objects.equals(first.getStatus(), 1)) {
            if (hasMaintenance && hasConflict) {
                return "该时段同时存在维护和冲突，已进入待核查。";
            }
            if (hasMaintenance) {
                return "该时段存在维护安排，已进入待核查。";
            }
            if (hasConflict) {
                return "该时段存在重复占用，已进入冲突待处理。";
            }
            return "待管理员审核。";
        }
        if (Objects.equals(first.getStatus(), 2)) {
            return "当前预约已通过。";
        }
        if (Objects.equals(first.getStatus(), 5)) {
            return "该预约今日已完成。";
        }
        return "今日预约记录。";
    }

    private long countByPeriodRange(List<DashboardReservationItemVo> reservations, int startPeriodNo, int endPeriodNo) {
        return reservations.stream()
            .filter(item -> item.getPeriodNo() != null && item.getPeriodNo() >= startPeriodNo && item.getPeriodNo() <= endPeriodNo)
            .map(DashboardReservationItemVo::getReservationId)
            .distinct()
            .count();
    }

    private boolean isUpcoming(String startTime) {
        LocalTime start = parseTime(startTime);
        if (start == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        LocalTime upper = now.plusHours(1);
        return !start.isBefore(now) && !start.isAfter(upper);
    }

    private LocalTime parseTime(String value) {
        return value == null || value.isBlank() ? null : LocalTime.parse(value, TIME_FORMATTER);
    }

    private String joinTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            return "--";
        }
        return startTime + " - " + endTime;
    }

    private String resolveReservationTypeLabel(Integer reservationType) {
        if (reservationType == null || reservationType == 3) {
            return "个人预约";
        }
        if (reservationType == 1) {
            return "课程实验";
        }
        if (reservationType == 2) {
            return "科研训练";
        }
        return "个人预约";
    }

    private String resolveStatusLabel(Integer status) {
        if (status == null || status == 1) {
            return "待审核";
        }
        if (status == 2) {
            return "已通过";
        }
        if (status == 3) {
            return "已驳回";
        }
        if (status == 4) {
            return "已取消";
        }
        if (status == 5) {
            return "已完成";
        }
        return "待审核";
    }

    private String lookupLabName(Map<Long, LabEntity> labMap, Long labId) {
        LabEntity lab = labMap.get(labId);
        return lab == null ? "未知实验室" : lab.getLabName();
    }

    private String resolveDeviceStatus(Integer status) {
        if (status == null) {
            return "状态异常";
        }
        if (status == 1) {
            return "正常";
        }
        if (status == 2) {
            return "维护中";
        }
        if (status == 3) {
            return "停用";
        }
        return "状态异常";
    }

    private String slotKey(Long labId, String reservationDate, Long periodId) {
        return labId + "#" + reservationDate + "#" + periodId;
    }

    private static final class OccupancyAggregate {
        private int occupiedSlots;
        private int totalOpenSlots;
        private int labCount;
    }
}

package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.reservation.ReservationRecommendationRequest;
import com.nlt.domain.entity.ClassPeriodEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.LabMaintenanceEntity;
import com.nlt.domain.entity.LabOpenSlotEntity;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import com.nlt.mapper.ClassPeriodMapper;
import com.nlt.mapper.LabMaintenanceMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.LabOpenSlotMapper;
import com.nlt.mapper.LabReservationSlotMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.ReservationRecommendationService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationRecommendationServiceImpl implements ReservationRecommendationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int SAME_LAB_NEARBY_DAY_LIMIT = 2;
    private static final int MAX_RESULTS = 12;

    private final ClassPeriodMapper classPeriodMapper;
    private final LabMaintenanceMapper labMaintenanceMapper;
    private final LabMapper labMapper;
    private final LabOpenSlotMapper labOpenSlotMapper;
    private final LabReservationSlotMapper labReservationSlotMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<SlotRecommendationItem> recommend(ReservationRecommendationRequest request, Long currentUserId) {
        if (request == null || request.getLabId() == null) {
            throw new BusinessException(400, "实验室不能为空");
        }
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new BusinessException(400, "请先选择需要推荐的时段");
        }

        LabEntity baseLab = labMapper.selectById(request.getLabId());
        if (baseLab == null || (baseLab.getDeleted() != null && baseLab.getDeleted() == 1)) {
            throw new BusinessException(404, "实验室不存在");
        }

        List<SlotKey> requestedSlots = request.getSlots().stream().map(item -> {
            LocalDate date = LocalDate.parse(item.getReservationDate(), DATE_FORMATTER);
            return new SlotKey(date, date.getDayOfWeek().getValue(), item.getPeriodId());
        }).distinct().toList();
        validateWithinNext21Days(requestedSlots);

        List<ClassPeriodEntity> periods = classPeriodMapper.selectActiveList();
        if (periods.isEmpty()) {
            throw new BusinessException(400, "尚未配置节次数据");
        }

        List<Long> allPeriodIds = periods.stream().map(ClassPeriodEntity::getId).toList();
        Map<Long, ClassPeriodEntity> periodMap = periods.stream()
            .collect(Collectors.toMap(ClassPeriodEntity::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        RoleFlags roleFlags = loadRoleFlags(currentUserId);
        AvailabilityCache cache = new AvailabilityCache(allPeriodIds, roleFlags);
        List<LabEntity> seriesLabs = labMapper.selectRecommendationCandidates(baseLab.getId(), request.getParticipantCount()).stream()
            .filter(lab -> isSameSeriesLab(baseLab, lab))
            .toList();
        Map<String, SlotRecommendationItem> deduplicated = new LinkedHashMap<>();
        for (SlotKey blocked : requestedSlots) {
            collectSameSeriesSamePeriod(deduplicated, cache, seriesLabs, blocked, periodMap);
            collectAdjacentPeriods(deduplicated, cache, baseLab, blocked, periods, periodMap, "当前实验室相邻节次可预约");
            collectAdjacentPeriods(deduplicated, cache, seriesLabs, blocked, periods, periodMap, "同系列实验室相邻节次可预约");
            collectSameLabNearbyDays(deduplicated, cache, baseLab, blocked, periodMap);
        }

        return deduplicated.values().stream()
            .limit(MAX_RESULTS)
            .toList();
    }

    private void collectSameSeriesSamePeriod(
        Map<String, SlotRecommendationItem> output,
        AvailabilityCache cache,
        List<LabEntity> labs,
        SlotKey blocked,
        Map<Long, ClassPeriodEntity> periodMap
    ) {
        for (LabEntity lab : labs) {
            if (cache.isAvailable(lab.getId(), blocked)) {
                putRecommendation(output, new SlotRecommendationItem(
                    lab.getId(),
                    lab.getLabName(),
                    blocked.reservationDate().format(DATE_FORMATTER),
                    blocked.periodId(),
                    periodName(periodMap, blocked.periodId()),
                    "同系列实验室相同节次可预约"
                ));
            }
        }
    }

    private void collectAdjacentPeriods(
        Map<String, SlotRecommendationItem> output,
        AvailabilityCache cache,
        LabEntity lab,
        SlotKey blocked,
        List<ClassPeriodEntity> periods,
        Map<Long, ClassPeriodEntity> periodMap,
        String reason
    ) {
        collectAdjacentPeriods(output, cache, List.of(lab), blocked, periods, periodMap, reason);
    }

    private void collectAdjacentPeriods(
        Map<String, SlotRecommendationItem> output,
        AvailabilityCache cache,
        List<LabEntity> labs,
        SlotKey blocked,
        List<ClassPeriodEntity> periods,
        Map<Long, ClassPeriodEntity> periodMap,
        String reason
    ) {
        for (LabEntity lab : labs) {
            for (Long adjacentPeriodId : adjacentPeriodIds(periods, blocked.periodId())) {
                SlotKey candidate = new SlotKey(blocked.reservationDate(), blocked.weekday(), adjacentPeriodId);
                if (cache.isAvailable(lab.getId(), candidate)) {
                    putRecommendation(output, new SlotRecommendationItem(
                        lab.getId(),
                        lab.getLabName(),
                        blocked.reservationDate().format(DATE_FORMATTER),
                        adjacentPeriodId,
                        periodName(periodMap, adjacentPeriodId),
                        reason
                    ));
                }
            }
        }
    }

    private void collectSameLabNearbyDays(
        Map<String, SlotRecommendationItem> output,
        AvailabilityCache cache,
        LabEntity baseLab,
        SlotKey blocked,
        Map<Long, ClassPeriodEntity> periodMap
    ) {
        LocalDate lastDate = LocalDate.now().plusDays(20);
        int found = 0;
        for (int offset = 1; offset <= 6 && found < SAME_LAB_NEARBY_DAY_LIMIT; offset++) {
            LocalDate candidateDate = blocked.reservationDate().plusDays(offset);
            if (candidateDate.isAfter(lastDate)) {
                break;
            }
            SlotKey candidate = new SlotKey(candidateDate, candidateDate.getDayOfWeek().getValue(), blocked.periodId());
            if (cache.isAvailable(baseLab.getId(), candidate)) {
                found++;
                putRecommendation(output, new SlotRecommendationItem(
                    baseLab.getId(),
                    baseLab.getLabName(),
                    candidateDate.format(DATE_FORMATTER),
                    blocked.periodId(),
                    periodName(periodMap, blocked.periodId()),
                    "同实验室近两天内相同节次可预约"
                ));
            }
        }
    }

    private void putRecommendation(Map<String, SlotRecommendationItem> output, SlotRecommendationItem item) {
        String key = item.getLabId() + "#" + item.getReservationDate() + "#" + item.getPeriodId();
        output.putIfAbsent(key, item);
    }

    private boolean isSameSeriesLab(LabEntity baseLab, LabEntity candidateLab) {
        if (candidateLab == null) {
            return false;
        }
        if (!Objects.equals(baseLab.getDepartmentId(), candidateLab.getDepartmentId())) {
            return false;
        }
        if (!Objects.equals(baseLab.getLabType(), candidateLab.getLabType())) {
            return false;
        }
        String baseSeries = normalizeSeriesName(baseLab.getLabName());
        String candidateSeries = normalizeSeriesName(candidateLab.getLabName());
        return !baseSeries.isBlank() && baseSeries.equalsIgnoreCase(candidateSeries);
    }

    private String normalizeSeriesName(String labName) {
        if (labName == null) {
            return "";
        }
        String normalized = labName.trim()
            .replaceAll("[0-9]+$", "")
            .replaceAll("[一二三四五六七八九十]+$", "")
            .replaceAll("[\\s\\-_/（）()]+$", "")
            .trim();
        return normalized;
    }

    private RoleFlags loadRoleFlags(Long currentUserId) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(currentUserId);
        return new RoleFlags(hasRole(roleCodes, "STUDENT"), hasRole(roleCodes, "TEACHER"));
    }

    private boolean hasRole(List<String> roleCodes, String roleCode) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(code ->
            roleCode.equalsIgnoreCase(code) || ("ROLE_" + roleCode).equalsIgnoreCase(code));
    }

    private void validateWithinNext21Days(List<SlotKey> slots) {
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(20);
        for (SlotKey key : slots) {
            if (key.reservationDate().isBefore(today) || key.reservationDate().isAfter(last)) {
                throw new BusinessException(400, "只能推荐今天起未来 21 天内的时段");
            }
        }
    }

    private String periodName(Map<Long, ClassPeriodEntity> periodMap, Long periodId) {
        ClassPeriodEntity period = periodMap.get(periodId);
        return period == null ? null : period.getPeriodName();
    }

    private List<Long> adjacentPeriodIds(List<ClassPeriodEntity> periods, Long periodId) {
        int currentIndex = -1;
        for (int i = 0; i < periods.size(); i++) {
            if (Objects.equals(periods.get(i).getId(), periodId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        if (currentIndex > 0) {
            result.add(periods.get(currentIndex - 1).getId());
        }
        if (currentIndex < periods.size() - 1) {
            result.add(periods.get(currentIndex + 1).getId());
        }
        return result;
    }

    private final class AvailabilityCache {

        private final List<Long> allPeriodIds;
        private final RoleFlags roleFlags;
        private final Map<String, Map<Long, LabOpenSlotEntity>> openSlotCache = new HashMap<>();
        private final Map<String, Set<Long>> maintenanceCache = new HashMap<>();
        private final Map<String, Set<Long>> reservedCache = new HashMap<>();

        private AvailabilityCache(List<Long> allPeriodIds, RoleFlags roleFlags) {
            this.allPeriodIds = allPeriodIds;
            this.roleFlags = roleFlags;
        }

        private boolean isAvailable(Long labId, SlotKey slot) {
            return isOpenAndAllowed(labId, slot.weekday(), slot.periodId())
                && !isMaintenance(labId, slot.reservationDate(), slot.periodId())
                && !isReserved(labId, slot.reservationDate(), slot.periodId());
        }

        private boolean isOpenAndAllowed(Long labId, int weekday, Long periodId) {
            Map<Long, LabOpenSlotEntity> openByPeriod = openSlotCache.computeIfAbsent(labId + "#" + weekday, key -> {
                List<LabOpenSlotEntity> slots = labOpenSlotMapper.selectActiveByLabAndWeekdaysAndPeriods(
                    labId,
                    List.of(weekday),
                    allPeriodIds
                );
                return slots.stream().collect(Collectors.toMap(LabOpenSlotEntity::getPeriodId, slot -> slot, (a, b) -> a));
            });
            LabOpenSlotEntity openSlot = openByPeriod.get(periodId);
            if (openSlot == null) {
                return false;
            }
            if (!roleFlags.isStudent() && !roleFlags.isTeacher()) {
                return true;
            }
            boolean allowStudent = openSlot.getAllowStudent() != null && openSlot.getAllowStudent() == 1;
            boolean allowTeacher = openSlot.getAllowTeacher() != null && openSlot.getAllowTeacher() == 1;
            if (roleFlags.isStudent() && roleFlags.isTeacher()) {
                return allowStudent || allowTeacher;
            }
            return roleFlags.isStudent() ? allowStudent : allowTeacher;
        }

        private boolean isMaintenance(Long labId, LocalDate date, Long periodId) {
            Set<Long> maintenancePeriods = maintenanceCache.computeIfAbsent(labId + "#" + date, key ->
                labMaintenanceMapper.selectByLabAndDateRange(labId, date, date).stream()
                    .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                    .map(LabMaintenanceEntity::getPeriodId)
                    .collect(Collectors.toSet())
            );
            return maintenancePeriods.contains(periodId);
        }

        private boolean isReserved(Long labId, LocalDate date, Long periodId) {
            Set<Long> reservedPeriods = reservedCache.computeIfAbsent(labId + "#" + date, key ->
                labReservationSlotMapper.selectReservedSlots(labId, date, date).stream()
                    .map(item -> item.getPeriodId())
                    .collect(Collectors.toSet())
            );
            return reservedPeriods.contains(periodId);
        }
    }

    private record SlotKey(LocalDate reservationDate, int weekday, Long periodId) { }

    private record RoleFlags(boolean isStudent, boolean isTeacher) { }
}

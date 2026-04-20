package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.reservation.ReservationApproveRequest;
import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.dto.reservation.ReservationRecommendationRequest;
import com.nlt.domain.dto.reservation.ReservationRejectRequest;
import com.nlt.domain.entity.ClassPeriodEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.LabMaintenanceEntity;
import com.nlt.domain.entity.LabOpenSlotEntity;
import com.nlt.domain.entity.LabReservationSlotEntity;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import com.nlt.domain.entity.ReservationEntity;
import com.nlt.domain.vo.reservation.ReservationDetailVo;
import com.nlt.domain.vo.reservation.ReservationSlotVo;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import com.nlt.mapper.ClassPeriodMapper;
import com.nlt.mapper.LabMaintenanceMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.LabOpenSlotMapper;
import com.nlt.mapper.LabReservationSlotMapper;
import com.nlt.mapper.ReservationAuditLogMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.ReservationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReservationMapper reservationMapper;
    private final ReservationAuditLogMapper reservationAuditLogMapper;
    private final LabReservationSlotMapper labReservationSlotMapper;
    private final LabMaintenanceMapper labMaintenanceMapper;
    private final LabOpenSlotMapper labOpenSlotMapper;
    private final LabMapper labMapper;
    private final UserRoleMapper userRoleMapper;

    // Keep for future extension (period name joins are handled in slot mapper)
    @SuppressWarnings("unused")
    private final ClassPeriodMapper classPeriodMapper;

    @Override
    public PageData<ReservationDetailVo> page(int pageNum, int pageSize, String reservationNo, Long labId,
        Long applicantUserId, Long approverUserId, Integer status, String reservationDate) {
        int offset = (pageNum - 1) * pageSize;
        LocalDate date = reservationDate == null || reservationDate.isBlank() ? null : LocalDate.parse(reservationDate, DATE_FORMATTER);
        List<ReservationEntity> list = reservationMapper.selectPage(offset, pageSize, reservationNo, labId,
            applicantUserId, approverUserId, status, date);
        long total = reservationMapper.countPage(reservationNo, labId, applicantUserId, approverUserId, status, date);
        return new PageData<>(attachSlots(list), total, pageNum, pageSize);
    }

    @Override
    public PageData<ReservationDetailVo> mine(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<ReservationEntity> list = reservationMapper.selectMine(userId, offset, pageSize);
        long total = reservationMapper.countMine(userId);
        return new PageData<>(attachSlots(list), total, pageNum, pageSize);
    }

    @Override
    public PageData<ReservationDetailVo> pendingAudit(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<ReservationEntity> list = reservationMapper.selectPendingAudit(offset, pageSize);
        long total = reservationMapper.countPendingAudit();
        return new PageData<>(attachSlots(list), total, pageNum, pageSize);
    }

    @Override
    public ReservationDetailVo getById(Long id) {
        ReservationEntity entity = reservationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "预订不存在");
        }
        List<ReservationSlotVo> slots = labReservationSlotMapper.selectDetailByReservationId(id);
        return toDetail(entity, slots);
    }

    @Transactional
    @Override
    public ReservationDetailVo create(ReservationCreateRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        LabEntity lab = loadAndValidateLab(request.getLabId());

        List<SlotKey> requestedSlots = normalizeRequestedSlots(request);
        validateWithinNext21Days(requestedSlots);

        Map<SlotKey, LabOpenSlotEntity> openSlotMap = loadOpenSlots(request.getLabId(), requestedSlots);
        validateRoleAllowed(openSlotMap, requestedSlots, currentUserId);
        validateNoMaintenance(lab.getId(), requestedSlots);
        validateNoReservationConflict(lab.getId(), requestedSlots);

        ReservationEntity entity = new ReservationEntity();
        entity.setReservationNo("RES" + System.currentTimeMillis());
        entity.setLabId(request.getLabId());
        entity.setApplicantUserId(currentUserId);
        entity.setApproverUserId(null);
        entity.setReservationType(request.getReservationType() == null ? 3 : request.getReservationType());
        entity.setPriorityLevel(request.getPriorityLevel() == null ? 3 : request.getPriorityLevel());
        entity.setUsagePurpose(request.getUsagePurpose());
        entity.setCourseOrProjectName(request.getCourseOrProjectName());
        entity.setParticipantCount(request.getParticipantCount() == null ? 1 : request.getParticipantCount());
        entity.setContactPhone(request.getContactPhone());
        entity.setStatus(1);
        entity.setRejectReason(null);
        entity.setCheckInTime(null);
        entity.setCheckOutTime(null);
        reservationMapper.insert(entity);

        List<LabReservationSlotEntity> slotEntities = requestedSlots.stream().map(key -> {
            LabReservationSlotEntity slot = new LabReservationSlotEntity();
            slot.setReservationId(entity.getId());
            slot.setLabId(entity.getLabId());
            slot.setReservationDate(key.reservationDate());
            slot.setWeekday(key.weekday());
            slot.setPeriodId(key.periodId());
            slot.setSlotStatus(1);
            return slot;
        }).toList();

        try {
            labReservationSlotMapper.deleteCanceledSlots(slotEntities);
            labReservationSlotMapper.insertBatch(slotEntities);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "所选节次存在冲突：该实验室在所选日期/节次已被占用");
        }

        insertAuditLog(entity.getId(), currentUserId, 1, "提交预约申请");
        return getById(entity.getId());
    }

    @Transactional
    @Override
    public ReservationDetailVo approve(Long id, ReservationApproveRequest request, Long currentUserId) {
        requireAdmin(currentUserId);
        ReservationEntity entity = requireReservation(id);
        if (!Objects.equals(entity.getStatus(), 1)) {
            throw new BusinessException(400, "仅待审批的预约可审核通过");
        }
        entity.setApproverUserId(currentUserId);
        entity.setStatus(2);
        entity.setRejectReason(null);
        reservationMapper.updateAuditResult(entity);
        insertAuditLog(id, currentUserId, 2, request == null ? null : request.getAuditComment());
        return getById(id);
    }

    @Transactional
    @Override
    public ReservationDetailVo reject(Long id, ReservationRejectRequest request, Long currentUserId) {
        requireAdmin(currentUserId);
        ReservationEntity entity = requireReservation(id);
        if (!Objects.equals(entity.getStatus(), 1)) {
            throw new BusinessException(400, "仅待审批的预约可审核拒绝");
        }
        entity.setApproverUserId(currentUserId);
        entity.setStatus(3);
        entity.setRejectReason(request == null ? null : request.getRejectReason());
        reservationMapper.updateAuditResult(entity);
        labReservationSlotMapper.cancelByReservationId(id);
        insertAuditLog(id, currentUserId, 3, request == null ? null : request.getAuditComment());
        return getById(id);
    }

    @Transactional
    @Override
    public ReservationDetailVo cancel(Long id, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        ReservationEntity entity = requireReservation(id);
        boolean isAdmin = isAdmin(currentUserId);
        if (!isAdmin && !Objects.equals(entity.getApplicantUserId(), currentUserId)) {
            throw new BusinessException(403, "仅申请人或管理员可取消该预约");
        }
        if (entity.getStatus() == null || (entity.getStatus() != 1 && entity.getStatus() != 2)) {
            throw new BusinessException(400, "仅待审批或已通过的预约可取消");
        }
        reservationMapper.cancel(id);
        labReservationSlotMapper.cancelByReservationId(id);
        insertAuditLog(id, currentUserId, 4, "取消预约");
        return getById(id);
    }

    @Transactional
    @Override
    public ReservationDetailVo checkIn(Long id, Long currentUserId) {
        requireReservation(id);
        reservationMapper.checkIn(id);
        insertAuditLog(id, currentUserId, 5, "签到");
        return getById(id);
    }

    @Transactional
    @Override
    public ReservationDetailVo checkOut(Long id, Long currentUserId) {
        requireReservation(id);
        reservationMapper.checkOut(id);
        insertAuditLog(id, currentUserId, 6, "签退/完成");
        return getById(id);
    }

    @Override
    public List<SlotRecommendationItem> recommend(ReservationRecommendationRequest request, Long currentUserId) {
        if (request == null || request.getLabId() == null) {
            throw new BusinessException(400, "labId 不能为空");
        }
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        LabEntity baseLab = loadAndValidateLab(request.getLabId());
        List<SlotKey> requestedSlots = request.getSlots().stream().map(item -> {
            LocalDate date = LocalDate.parse(item.getReservationDate(), DATE_FORMATTER);
            int weekday = date.getDayOfWeek().getValue();
            return new SlotKey(date, weekday, item.getPeriodId());
        }).distinct().toList();
        validateWithinNext21Days(requestedSlots);

        List<ClassPeriodEntity> periods = classPeriodMapper.selectActiveList();
        if (periods.isEmpty()) {
            throw new BusinessException(400, "未配置课程节次");
        }
        List<Long> allPeriodIds = periods.stream().map(ClassPeriodEntity::getId).toList();
        Map<Long, ClassPeriodEntity> periodMap = periods.stream()
            .collect(Collectors.toMap(ClassPeriodEntity::getId, p -> p, (a, b) -> a));

        RoleFlags flags = loadRoleFlags(currentUserId);
        AvailabilityCache cache = new AvailabilityCache(allPeriodIds, flags);

        List<SlotRecommendationItem> output = new ArrayList<>();
        for (SlotKey blocked : requestedSlots) {
            if (cache.isAvailable(baseLab.getId(), blocked)) {
                continue;
            }
            List<SlotRecommendationItem> slotRecs = new ArrayList<>();

            // 1) Same lab adjacent free periods
            for (Long candidatePeriodId : adjacentPeriodIds(periods, blocked.periodId())) {
                SlotKey candidate = new SlotKey(blocked.reservationDate(), blocked.weekday(), candidatePeriodId);
                if (cache.isAvailable(baseLab.getId(), candidate)) {
                    slotRecs.add(new SlotRecommendationItem(baseLab.getId(), baseLab.getLabName(),
                        blocked.reservationDate().format(DATE_FORMATTER),
                        candidatePeriodId,
                        periodName(periodMap, candidatePeriodId),
                        "同实验室相邻空闲节次"));
                }
            }
            if (!slotRecs.isEmpty()) {
                output.addAll(slotRecs);
                continue;
            }

            // 2) Same department & lab type, same date & same period
            List<LabEntity> sameDeptTypeLabs = labMapper.selectRecommendationCandidates(baseLab.getId(), request.getParticipantCount()).stream()
                .filter(l -> Objects.equals(l.getDepartmentId(), baseLab.getDepartmentId()))
                .filter(l -> Objects.equals(l.getLabType(), baseLab.getLabType()))
                .toList();
            for (LabEntity lab : sameDeptTypeLabs) {
                if (cache.isAvailable(lab.getId(), blocked)) {
                    slotRecs.add(new SlotRecommendationItem(lab.getId(), lab.getLabName(),
                        blocked.reservationDate().format(DATE_FORMATTER),
                        blocked.periodId(),
                        periodName(periodMap, blocked.periodId()),
                        "同院系同类型实验室，相同节次"));
                }
                if (slotRecs.size() >= 5) {
                    break;
                }
            }
            if (!slotRecs.isEmpty()) {
                output.addAll(slotRecs);
                continue;
            }

            // 3) Other labs, same period; otherwise adjacent periods
            List<LabEntity> anyLabs = labMapper.selectRecommendationCandidates(baseLab.getId(), request.getParticipantCount());
            for (LabEntity lab : anyLabs) {
                if (cache.isAvailable(lab.getId(), blocked)) {
                    slotRecs.add(new SlotRecommendationItem(lab.getId(), lab.getLabName(),
                        blocked.reservationDate().format(DATE_FORMATTER),
                        blocked.periodId(),
                        periodName(periodMap, blocked.periodId()),
                        "其他可用实验室，相同节次"));
                } else {
                    for (Long candidatePeriodId : adjacentPeriodIds(periods, blocked.periodId())) {
                        SlotKey candidate = new SlotKey(blocked.reservationDate(), blocked.weekday(), candidatePeriodId);
                        if (cache.isAvailable(lab.getId(), candidate)) {
                            slotRecs.add(new SlotRecommendationItem(lab.getId(), lab.getLabName(),
                                blocked.reservationDate().format(DATE_FORMATTER),
                                candidatePeriodId,
                                periodName(periodMap, candidatePeriodId),
                                "其他可用实验室，相邻节次"));
                        }
                        if (slotRecs.size() >= 5) {
                            break;
                        }
                    }
                }
                if (slotRecs.size() >= 5) {
                    break;
                }
            }
            output.addAll(slotRecs);
        }

        return output.stream().limit(10).toList();
    }

    private RoleFlags loadRoleFlags(Long currentUserId) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(currentUserId);
        boolean isStudent = hasRole(roleCodes, "STUDENT");
        boolean isTeacher = hasRole(roleCodes, "TEACHER");
        return new RoleFlags(isStudent, isTeacher);
    }

    private String periodName(Map<Long, ClassPeriodEntity> periodMap, Long periodId) {
        ClassPeriodEntity p = periodMap.get(periodId);
        return p == null ? null : p.getPeriodName();
    }

    private List<Long> adjacentPeriodIds(List<ClassPeriodEntity> periods, Long periodId) {
        int idx = -1;
        for (int i = 0; i < periods.size(); i++) {
            if (Objects.equals(periods.get(i).getId(), periodId)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        if (idx - 1 >= 0) {
            result.add(periods.get(idx - 1).getId());
        }
        if (idx + 1 < periods.size()) {
            result.add(periods.get(idx + 1).getId());
        }
        return result;
    }

    private class AvailabilityCache {
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
            if (!isOpenAndAllowed(labId, slot.weekday(), slot.periodId())) {
                return false;
            }
            if (isMaintenance(labId, slot.reservationDate(), slot.periodId())) {
                return false;
            }
            return !isReserved(labId, slot.reservationDate(), slot.periodId());
        }

        private boolean isOpenAndAllowed(Long labId, int weekday, Long periodId) {
            Map<Long, LabOpenSlotEntity> openByPeriod = openSlotCache.computeIfAbsent(labId + "#" + weekday, k -> {
                List<LabOpenSlotEntity> list = labOpenSlotMapper.selectActiveByLabAndWeekdaysAndPeriods(labId, List.of(weekday), allPeriodIds);
                return list.stream().collect(Collectors.toMap(LabOpenSlotEntity::getPeriodId, s -> s, (a, b) -> a));
            });
            LabOpenSlotEntity open = openByPeriod.get(periodId);
            if (open == null) {
                return false;
            }
            if (!roleFlags.isStudent() && !roleFlags.isTeacher()) {
                return true;
            }
            boolean allowStudent = open.getAllowStudent() != null && open.getAllowStudent() == 1;
            boolean allowTeacher = open.getAllowTeacher() != null && open.getAllowTeacher() == 1;
            if (roleFlags.isStudent() && roleFlags.isTeacher()) {
                return allowStudent || allowTeacher;
            }
            if (roleFlags.isStudent()) {
                return allowStudent;
            }
            return allowTeacher;
        }

        private boolean isMaintenance(Long labId, LocalDate date, Long periodId) {
            Set<Long> set = maintenanceCache.computeIfAbsent(labId + "#" + date, k -> {
                return labMaintenanceMapper.selectByLabAndDateRange(labId, date, date).stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                    .map(LabMaintenanceEntity::getPeriodId)
                    .collect(Collectors.toSet());
            });
            return set.contains(periodId);
        }

        private boolean isReserved(Long labId, LocalDate date, Long periodId) {
            Set<Long> set = reservedCache.computeIfAbsent(labId + "#" + date, k -> {
                return labReservationSlotMapper.selectReservedSlots(labId, date, date).stream()
                    .map(r -> r.getPeriodId())
                    .collect(Collectors.toSet());
            });
            return set.contains(periodId);
        }
    }

    private record RoleFlags(boolean isStudent, boolean isTeacher) { }

    private List<ReservationDetailVo> attachSlots(List<ReservationEntity> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        List<Long> reservationIds = reservations.stream().map(ReservationEntity::getId).filter(Objects::nonNull).toList();
        List<ReservationSlotVo> slotVos = labReservationSlotMapper.selectDetailByReservationIds(reservationIds);
        Map<Long, List<ReservationSlotVo>> slotsByReservationId = slotVos.stream()
            .collect(Collectors.groupingBy(ReservationSlotVo::getReservationId));
        return reservations.stream()
            .map(r -> toDetail(r, slotsByReservationId.getOrDefault(r.getId(), List.of())))
            .toList();
    }

    private ReservationDetailVo toDetail(ReservationEntity entity, List<ReservationSlotVo> slots) {
        return new ReservationDetailVo(
            entity.getId(),
            entity.getReservationNo(),
            entity.getLabId(),
            entity.getApplicantUserId(),
            entity.getApproverUserId(),
            entity.getReservationType(),
            entity.getPriorityLevel(),
            entity.getUsagePurpose(),
            entity.getCourseOrProjectName(),
            entity.getParticipantCount(),
            entity.getContactPhone(),
            entity.getStatus(),
            entity.getRejectReason(),
            formatDateTime(entity.getCheckInTime()),
            formatDateTime(entity.getCheckOutTime()),
            formatDateTime(entity.getCreatedAt()),
            formatDateTime(entity.getUpdatedAt()),
            slots
        );
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private ReservationEntity requireReservation(Long id) {
        ReservationEntity entity = reservationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "预订不存在");
        }
        return entity;
    }

    private LabEntity loadAndValidateLab(Long labId) {
        if (labId == null) {
            throw new BusinessException(400, "实验室ID不能为空");
        }
        LabEntity lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException(404, "实验室不存在");
        }
        if (lab.getDeleted() != null && lab.getDeleted() == 1) {
            throw new BusinessException(400, "实验室已删除");
        }
        if (lab.getOpenStatus() == null || lab.getOpenStatus() != 1) {
            throw new BusinessException(400, "实验室未开放");
        }
        if (lab.getLabStatus() == null || lab.getLabStatus() != 1) {
            throw new BusinessException(400, "实验室维护中");
        }
        return lab;
    }

    private List<SlotKey> normalizeRequestedSlots(ReservationCreateRequest request) {
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new BusinessException(400, "预约时段不能为空");
        }
        return request.getSlots().stream().map(item -> {
            LocalDate date = LocalDate.parse(item.getReservationDate(), DATE_FORMATTER);
            int weekday = date.getDayOfWeek().getValue();
            return new SlotKey(date, weekday, item.getPeriodId());
        }).distinct().toList();
    }

    private void validateWithinNext21Days(List<SlotKey> slots) {
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(20);
        for (SlotKey key : slots) {
            if (key.reservationDate().isBefore(today) || key.reservationDate().isAfter(last)) {
                throw new BusinessException(400,
                    "预约日期超出允许范围（仅限今天起21天内）: " + key.reservationDate().format(DATE_FORMATTER));
            }
        }
    }

    private Map<SlotKey, LabOpenSlotEntity> loadOpenSlots(Long labId, List<SlotKey> requestedSlots) {
        Set<Integer> weekdays = requestedSlots.stream().map(SlotKey::weekday).collect(Collectors.toSet());
        Set<Long> periodIds = requestedSlots.stream().map(SlotKey::periodId).collect(Collectors.toSet());
        List<LabOpenSlotEntity> openSlots = labOpenSlotMapper.selectActiveByLabAndWeekdaysAndPeriods(labId,
            weekdays.stream().toList(), periodIds.stream().toList());
        Map<SlotKey, LabOpenSlotEntity> map = new HashMap<>();
        for (LabOpenSlotEntity slot : openSlots) {
            map.put(new SlotKey(null, slot.getWeekday(), slot.getPeriodId()), slot);
        }
        for (SlotKey key : requestedSlots) {
            LabOpenSlotEntity matched = map.get(new SlotKey(null, key.weekday(), key.periodId()));
            if (matched == null) {
                throw new BusinessException(400, "该时段未开放：星期" + key.weekday() + "，节次ID=" + key.periodId());
            }
        }
        return map;
    }

    private void validateRoleAllowed(Map<SlotKey, LabOpenSlotEntity> openSlotMap, List<SlotKey> requestedSlots, Long currentUserId) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(currentUserId);
        boolean isStudent = hasRole(roleCodes, "STUDENT");
        boolean isTeacher = hasRole(roleCodes, "TEACHER");
        if (!isStudent && !isTeacher) {
            return;
        }
        for (SlotKey key : requestedSlots) {
            LabOpenSlotEntity slot = openSlotMap.get(new SlotKey(null, key.weekday(), key.periodId()));
            if (slot == null) {
                continue;
            }
            boolean allowStudent = slot.getAllowStudent() != null && slot.getAllowStudent() == 1;
            boolean allowTeacher = slot.getAllowTeacher() != null && slot.getAllowTeacher() == 1;
            if (isStudent && isTeacher) {
                if (!allowStudent && !allowTeacher) {
                    throw new BusinessException(400, "当前用户角色不允许预约该时段");
                }
            } else if (isStudent && !allowStudent) {
                throw new BusinessException(400, "学生不允许预约该时段");
            } else if (isTeacher && !allowTeacher) {
                throw new BusinessException(400, "教师不允许预约该时段");
            }
        }
    }

    private void validateNoMaintenance(Long labId, List<SlotKey> requestedSlots) {
        LocalDate min = requestedSlots.stream().map(SlotKey::reservationDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = requestedSlots.stream().map(SlotKey::reservationDate).max(LocalDate::compareTo).orElseThrow();
        List<LabMaintenanceEntity> maintenances = labMaintenanceMapper.selectByLabAndDateRange(labId, min, max).stream()
            .filter(m -> m.getStatus() != null && m.getStatus() == 1)
            .toList();
        if (maintenances.isEmpty()) {
            return;
        }
        Map<String, LabMaintenanceEntity> map = maintenances.stream()
            .collect(Collectors.toMap(m -> key(m.getMaintenanceDate(), m.getPeriodId()), m -> m, (a, b) -> a));
        for (SlotKey key : requestedSlots) {
            if (map.containsKey(key(key.reservationDate(), key.periodId()))) {
                throw new BusinessException(400, "该时段实验室维护中：" + key.reservationDate().format(DATE_FORMATTER)
                    + "，节次ID=" + key.periodId());
            }
        }
    }

    private void validateNoReservationConflict(Long labId, List<SlotKey> requestedSlots) {
        LocalDate min = requestedSlots.stream().map(SlotKey::reservationDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = requestedSlots.stream().map(SlotKey::reservationDate).max(LocalDate::compareTo).orElseThrow();
        Map<String, Boolean> reservedMap = labReservationSlotMapper.selectReservedSlots(labId, min, max).stream()
            .collect(Collectors.toMap(r -> key(r.getReservationDate(), r.getPeriodId()), r -> true, (a, b) -> a));
        for (SlotKey key : requestedSlots) {
            if (reservedMap.containsKey(key(key.reservationDate(), key.periodId()))) {
                throw new BusinessException(400, "该节次已被预约：" + key.reservationDate().format(DATE_FORMATTER)
                    + "，节次ID=" + key.periodId());
            }
        }
    }

    private void insertAuditLog(Long reservationId, Long currentUserId, Integer action, String comment) {
        if (currentUserId == null) {
            return;
        }
        ReservationAuditLogEntity logEntity = new ReservationAuditLogEntity();
        logEntity.setReservationId(reservationId);
        logEntity.setAuditUserId(currentUserId);
        logEntity.setAuditAction(action);
        logEntity.setAuditComment(comment);
        reservationAuditLogMapper.insert(logEntity);
    }

    private void requireAdmin(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (!isAdmin(currentUserId)) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    private boolean isAdmin(Long userId) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
        return hasRole(roleCodes, "ADMIN");
    }

    private boolean hasRole(List<String> roleCodes, String roleCode) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(code ->
            roleCode.equalsIgnoreCase(code) || ("ROLE_" + roleCode).equalsIgnoreCase(code));
    }

    private String key(LocalDate date, Long periodId) {
        return date.toString() + "#" + periodId;
    }

    private record SlotKey(LocalDate reservationDate, int weekday, Long periodId) { }
}

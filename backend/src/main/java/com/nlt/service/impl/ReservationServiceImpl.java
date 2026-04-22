package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
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
import com.nlt.domain.vo.reservation.ReservationApplyResponse;
import com.nlt.domain.vo.reservation.ReservationConflictReservationVo;
import com.nlt.domain.vo.reservation.ReservationConflictRowVo;
import com.nlt.domain.vo.reservation.ReservationConflictSlotVo;
import com.nlt.domain.vo.reservation.ReservationDetailVo;
import com.nlt.domain.vo.reservation.ReservationSlotVo;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import com.nlt.domain.vo.reservation.SlotStatusItem;
import com.nlt.domain.vo.reservation.SlotStatusResponse;
import com.nlt.domain.vo.schedule.ReservedSlotRow;
import com.nlt.mapper.ClassPeriodMapper;
import com.nlt.mapper.LabMaintenanceMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.LabOpenSlotMapper;
import com.nlt.mapper.LabReservationSlotMapper;
import com.nlt.mapper.ReservationAuditLogMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.ReservationConflictService;
import com.nlt.service.ReservationRecommendationService;
import com.nlt.service.ReservationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final ReservationConflictService reservationConflictService;
    private final ReservationRecommendationService reservationRecommendationService;
    private final ClassPeriodMapper classPeriodMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public PageData<ReservationDetailVo> page(int pageNum, int pageSize, String reservationNo, Long labId,
        Long applicantUserId, Long approverUserId, Integer status, String reservationDate, Boolean conflictOnly) {
        int offset = (pageNum - 1) * pageSize;
        LocalDate date = reservationDate == null || reservationDate.isBlank() ? null : LocalDate.parse(reservationDate, DATE_FORMATTER);
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        List<ReservationEntity> list = reservationMapper.selectPage(offset, pageSize, reservationNo, labId,
            applicantUserId, approverUserId, status, date, departmentId, Boolean.TRUE.equals(conflictOnly));
        long total = reservationMapper.countPage(reservationNo, labId, applicantUserId, approverUserId, status, date, departmentId,
            Boolean.TRUE.equals(conflictOnly));
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
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        List<ReservationEntity> list = reservationMapper.selectPendingAudit(offset, pageSize, departmentId);
        long total = reservationMapper.countPendingAudit(departmentId);
        return new PageData<>(attachSlots(list), total, pageNum, pageSize);
    }

    @Override
    public PageData<ReservationConflictSlotVo> conflictPage(int pageNum, int pageSize) {
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        List<ReservationConflictRowVo> rows = labReservationSlotMapper.selectConflictRowsByDepartment(departmentId);
        Map<Long, Integer> periodOrderMap = classPeriodMapper.selectActiveList().stream()
            .collect(Collectors.toMap(ClassPeriodEntity::getId, ClassPeriodEntity::getPeriodNo, (left, right) -> left));
        Map<String, List<ReservationConflictRowVo>> grouped = rows.stream()
            .collect(Collectors.groupingBy(row -> conflictKey(row.getLabId(), row.getReservationDate(), row.getPeriodId()),
                LinkedHashMap::new, Collectors.toList()));

        List<ReservationConflictSlotVo> all = grouped.values().stream()
            .filter(list -> list.size() > 1)
            .map(this::toConflictSlotVo)
            .sorted((left, right) -> {
                int dateCompare = left.getReservationDate().compareTo(right.getReservationDate());
                if (dateCompare != 0) return dateCompare;
                int periodCompare = Integer.compare(periodOrderMap.getOrDefault(left.getPeriodId(), Integer.MAX_VALUE),
                    periodOrderMap.getOrDefault(right.getPeriodId(), Integer.MAX_VALUE));
                if (periodCompare != 0) return periodCompare;
                return left.getLabName().compareTo(right.getLabName());
            })
            .toList();

        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        int toIndex = Math.min(all.size(), fromIndex + pageSize);
        List<ReservationConflictSlotVo> pageList = fromIndex >= toIndex ? List.of() : all.subList(fromIndex, toIndex);
        return new PageData<>(pageList, all.size(), pageNum, pageSize);
    }

    @Override
    public ReservationDetailVo getById(Long id) {
        ReservationEntity entity = requireReservation(id);
        List<ReservationSlotVo> slots = labReservationSlotMapper.selectDetailByReservationId(id);
        return toDetail(entity, slots);
    }

    @Transactional
    @Override
    public ReservationApplyResponse apply(ReservationCreateRequest request, Long currentUserId) {
        requireLogin(currentUserId);
        loadAndValidateLab(request.getLabId());
        validateStudentReservationType(currentUserId, request.getReservationType());

        List<SlotKey> requestedSlots = normalizeRequestedSlots(request);
        validateWithinNext21Days(requestedSlots);
        Map<SlotKey, LabOpenSlotEntity> openSlotMap = loadOpenSlots(request.getLabId(), requestedSlots);
        validateRoleAllowed(openSlotMap, requestedSlots, currentUserId);
        validateNoMaintenance(request.getLabId(), requestedSlots);

        ReservationConflictService.ReservationConflictResult conflictResult =
            reservationConflictService.analyze(currentUserId, request);
        if (conflictResult.selfPendingConflict() || conflictResult.approvedConflict()) {
            return new ReservationApplyResponse(
                false,
                conflictResult.selfPendingConflict() ? "PENDING_SELF" : "APPROVED",
                true,
                conflictResult.conflictNote(),
                null,
                conflictResult.recommendations()
            );
        }

        ReservationDetailVo reservation = persistReservation(request, currentUserId, requestedSlots);
        String currentStatus = conflictResult.pendingOthersConflict()
            ? (conflictResult.higherPriorityThanOthers() ? "PENDING_PRIORITY" : "PENDING")
            : "PENDING";

        return new ReservationApplyResponse(
            true,
            currentStatus,
            conflictResult.pendingOthersConflict(),
            conflictResult.conflictNote(),
            reservation,
            conflictResult.recommendations()
        );
    }

    @Transactional
    @Override
    public ReservationDetailVo create(ReservationCreateRequest request, Long currentUserId) {
        requireLogin(currentUserId);
        loadAndValidateLab(request.getLabId());
        validateStudentReservationType(currentUserId, request.getReservationType());

        List<SlotKey> requestedSlots = normalizeRequestedSlots(request);
        validateWithinNext21Days(requestedSlots);
        Map<SlotKey, LabOpenSlotEntity> openSlotMap = loadOpenSlots(request.getLabId(), requestedSlots);
        validateRoleAllowed(openSlotMap, requestedSlots, currentUserId);
        validateNoMaintenance(request.getLabId(), requestedSlots);
        validateNoReservationConflict(request.getLabId(), requestedSlots, currentUserId);

        return persistReservation(request, currentUserId, requestedSlots);
    }

    @Override
    public SlotStatusResponse slotStatus(Long labId, String date, Long currentUserId) {
        requireLogin(currentUserId);
        loadAndValidateLab(labId);

        LocalDate targetDate = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date, DATE_FORMATTER);
        validateWithinNext21Days(List.of(new SlotKey(targetDate, targetDate.getDayOfWeek().getValue(), 0L)));

        List<ClassPeriodEntity> periods = classPeriodMapper.selectActiveList();
        if (periods.isEmpty()) {
            throw new BusinessException(400, "尚未配置节次");
        }

        List<SlotKey> daySlots = periods.stream()
            .map(period -> new SlotKey(targetDate, targetDate.getDayOfWeek().getValue(), period.getId()))
            .toList();
        Map<SlotKey, LabOpenSlotEntity> openSlotMap = loadOpenSlots(labId, daySlots);

        Map<String, LabMaintenanceEntity> maintenanceMap = labMaintenanceMapper.selectByLabAndDateRange(labId, targetDate, targetDate).stream()
            .filter(item -> item.getStatus() != null && item.getStatus() == 1)
            .collect(Collectors.toMap(item -> key(item.getMaintenanceDate(), item.getPeriodId()), item -> item, (a, b) -> a));

        Map<String, ReservedSlotRow> reservedMap = labReservationSlotMapper.selectReservedSlots(labId, targetDate, targetDate).stream()
            .collect(Collectors.toMap(
                item -> key(item.getReservationDate(), item.getPeriodId()),
                item -> item,
                (left, right) -> choosePreferredRow(left, right, currentUserId)
            ));

        List<SlotStatusItem> items = new ArrayList<>();
        for (ClassPeriodEntity period : periods) {
            SlotKey slotKey = new SlotKey(targetDate, targetDate.getDayOfWeek().getValue(), period.getId());
            LabOpenSlotEntity openSlot = openSlotMap.get(new SlotKey(null, slotKey.weekday(), slotKey.periodId()));
            boolean roleAllowed = isRoleAllowedForOpenSlot(openSlot, currentUserId);
            String slotMapKey = key(targetDate, period.getId());

            if (!roleAllowed) {
                items.add(new SlotStatusItem(period.getId(), period.getPeriodName(), "UNAVAILABLE", "不可用", "当前节次未开放"));
                continue;
            }

            LabMaintenanceEntity maintenance = maintenanceMap.get(slotMapKey);
            if (maintenance != null) {
                items.add(new SlotStatusItem(period.getId(), period.getPeriodName(), "UNAVAILABLE", "不可用", maintenance.getReason()));
                continue;
            }

            ReservedSlotRow reserved = reservedMap.get(slotMapKey);
            if (reserved == null) {
                items.add(new SlotStatusItem(period.getId(), period.getPeriodName(), "AVAILABLE", "可预约", null));
                continue;
            }

            if (Objects.equals(reserved.getReservationStatus(), 2) || Objects.equals(reserved.getReservationStatus(), 5)) {
                items.add(new SlotStatusItem(period.getId(), period.getPeriodName(), "APPROVED", "已占用",
                    "预约单号：" + reserved.getReservationNo()));
            } else if (Objects.equals(reserved.getApplicantUserId(), currentUserId)) {
                items.add(new SlotStatusItem(period.getId(), period.getPeriodName(), "PENDING_SELF", "待审核（本人）", "你已申请过该时段"));
            } else {
                items.add(new SlotStatusItem(period.getId(), period.getPeriodName(), "PENDING_OTHERS", "可申请",
                    "已有他人待审核，提交后将进入冲突判定"));
            }
        }

        return new SlotStatusResponse(labId, targetDate.format(DATE_FORMATTER), items);
    }

    @Transactional
    @Override
    public ReservationDetailVo approve(Long id, ReservationApproveRequest request, Long currentUserId) {
        requireAdmin(currentUserId);
        ReservationEntity entity = requireReservation(id);
        if (!Objects.equals(entity.getStatus(), 1)) {
            throw new BusinessException(400, "仅待审核预约可审批通过");
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
            throw new BusinessException(400, "仅待审核预约可驳回");
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
        requireLogin(currentUserId);
        ReservationEntity entity = requireReservation(id);
        boolean isAdmin = isAdmin(currentUserId);
        if (!isAdmin && !Objects.equals(entity.getApplicantUserId(), currentUserId)) {
            throw new BusinessException(403, "仅申请人或管理员可取消预约");
        }
        if (entity.getStatus() == null || (entity.getStatus() != 1 && entity.getStatus() != 2)) {
            throw new BusinessException(400, "仅待审核或已通过预约可取消");
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
        insertAuditLog(id, currentUserId, 6, "签退");
        return getById(id);
    }

    @Override
    public List<SlotRecommendationItem> recommend(ReservationRecommendationRequest request, Long currentUserId) {
        return reservationRecommendationService.recommend(request, currentUserId);
    }

    private ReservationDetailVo persistReservation(ReservationCreateRequest request, Long currentUserId, List<SlotKey> requestedSlots) {
        ReservationEntity entity = new ReservationEntity();
        entity.setReservationNo("RES" + System.currentTimeMillis());
        entity.setLabId(request.getLabId());
        entity.setApplicantUserId(currentUserId);
        entity.setApproverUserId(null);
        entity.setReservationType(request.getReservationType() == null ? 3 : request.getReservationType());
        entity.setPriorityLevel(resolvePriorityLevel(request));
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
            LabReservationSlotEntity slotEntity = new LabReservationSlotEntity();
            slotEntity.setReservationId(entity.getId());
            slotEntity.setLabId(entity.getLabId());
            slotEntity.setReservationDate(key.reservationDate());
            slotEntity.setWeekday(key.weekday());
            slotEntity.setPeriodId(key.periodId());
            slotEntity.setSlotStatus(1);
            return slotEntity;
        }).toList();

        try {
            labReservationSlotMapper.deleteCanceledSlots(slotEntities);
            labReservationSlotMapper.insertBatch(slotEntities);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "该时间段已经被预约，请重新选择。");
        }

        insertAuditLog(entity.getId(), currentUserId, 1, "提交预约申请");
        return getById(entity.getId());
    }

    private int resolvePriorityLevel(ReservationCreateRequest request) {
        if (request.getPriorityLevel() != null && request.getPriorityLevel() > 0) {
            return request.getPriorityLevel();
        }
        Integer reservationType = request.getReservationType();
        if (reservationType != null && reservationType == 1) {
            return 1;
        }
        if (reservationType != null && reservationType == 2) {
            return 2;
        }
        return 3;
    }

    private void validateStudentReservationType(Long currentUserId, Integer reservationType) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(currentUserId);
        if (hasRole(roleCodes, "STUDENT") && (reservationType == null || reservationType != 3)) {
            throw new BusinessException(400, "学生仅支持个人预约");
        }
    }

    private ReservedSlotRow choosePreferredRow(ReservedSlotRow left, ReservedSlotRow right, Long currentUserId) {
        if (isApprovedLike(left.getReservationStatus()) && !isApprovedLike(right.getReservationStatus())) {
            return left;
        }
        if (!isApprovedLike(left.getReservationStatus()) && isApprovedLike(right.getReservationStatus())) {
            return right;
        }
        boolean leftSelf = currentUserId != null && currentUserId.equals(left.getApplicantUserId());
        boolean rightSelf = currentUserId != null && currentUserId.equals(right.getApplicantUserId());
        if (leftSelf && !rightSelf) {
            return left;
        }
        if (rightSelf && !leftSelf) {
            return right;
        }
        if (Objects.equals(left.getApplicantUserId(), right.getApplicantUserId())) {
            return left;
        }
        return left;
    }

    private boolean isApprovedLike(Integer status) {
        return Objects.equals(status, 2) || Objects.equals(status, 5);
    }

    private List<ReservationDetailVo> attachSlots(List<ReservationEntity> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        List<Long> reservationIds = reservations.stream().map(ReservationEntity::getId).filter(Objects::nonNull).toList();
        List<ReservationSlotVo> slotVos = labReservationSlotMapper.selectDetailByReservationIds(reservationIds);
        Map<Long, List<ReservationSlotVo>> slotsByReservationId = slotVos.stream()
            .collect(Collectors.groupingBy(ReservationSlotVo::getReservationId));
        return reservations.stream()
            .map(item -> toDetail(item, slotsByReservationId.getOrDefault(item.getId(), List.of())))
            .toList();
    }

    private ReservationDetailVo toDetail(ReservationEntity entity, List<ReservationSlotVo> slots) {
        return new ReservationDetailVo(
            entity.getId(),
            entity.getReservationNo(),
            entity.getLabId(),
            entity.getApplicantUserId(),
            entity.getApplicantName(),
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

    private ReservationConflictSlotVo toConflictSlotVo(List<ReservationConflictRowVo> rows) {
        ReservationConflictRowVo first = rows.get(0);
        List<ReservationConflictReservationVo> reservations = rows.stream()
            .map(row -> new ReservationConflictReservationVo(
                row.getReservationId(),
                row.getReservationNo(),
                row.getApplicantUserId(),
                row.getApplicantName(),
                row.getReservationType(),
                row.getPriorityLevel(),
                row.getReservationStatus(),
                row.getCreatedAt(),
                row.getUsagePurpose(),
                row.getCourseOrProjectName()
            ))
            .toList();
        return new ReservationConflictSlotVo(
            first.getLabId(),
            first.getLabName(),
            first.getReservationDate().format(DATE_FORMATTER),
            first.getReservationDate().getDayOfWeek().getValue(),
            first.getPeriodId(),
            first.getPeriodName(),
            first.getStartTime(),
            first.getEndTime(),
            reservations.size(),
            reservations
        );
    }

    private String conflictKey(Long labId, LocalDate reservationDate, Long periodId) {
        return labId + "|" + reservationDate + "|" + periodId;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private ReservationEntity requireReservation(Long id) {
        ReservationEntity entity = reservationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "预约不存在");
        }
        LabEntity lab = labMapper.selectById(entity.getLabId());
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "预约不存在");
        }
        currentUserScopeService.ensureDepartmentAccessible(lab.getDepartmentId(), "预约不存在");
        return entity;
    }

    private LabEntity loadAndValidateLab(Long labId) {
        if (labId == null) {
            throw new BusinessException(400, "实验室不能为空");
        }
        LabEntity lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException(404, "实验室不存在");
        }
        currentUserScopeService.ensureDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
        if (lab.getDeleted() != null && lab.getDeleted() == 1) {
            throw new BusinessException(400, "实验室已删除");
        }
        if (lab.getOpenStatus() == null || lab.getOpenStatus() != 1) {
            throw new BusinessException(400, "实验室当前未开放");
        }
        if (lab.getLabStatus() == null || lab.getLabStatus() != 1) {
            throw new BusinessException(400, "实验室当前不可预约");
        }
        return lab;
    }

    private List<SlotKey> normalizeRequestedSlots(ReservationCreateRequest request) {
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new BusinessException(400, "请选择预约节次");
        }
        return request.getSlots().stream().map(item -> {
            LocalDate date = LocalDate.parse(item.getReservationDate(), DATE_FORMATTER);
            return new SlotKey(date, date.getDayOfWeek().getValue(), item.getPeriodId());
        }).distinct().toList();
    }

    private void validateWithinNext21Days(List<SlotKey> slots) {
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(20);
        for (SlotKey key : slots) {
            if (key.reservationDate().isBefore(today) || key.reservationDate().isAfter(last)) {
                throw new BusinessException(400, "预约日期超出允许范围：仅支持今天起未来 21 天");
            }
        }
    }

    private Map<SlotKey, LabOpenSlotEntity> loadOpenSlots(Long labId, List<SlotKey> requestedSlots) {
        Set<Integer> weekdays = requestedSlots.stream().map(SlotKey::weekday).collect(Collectors.toSet());
        Set<Long> periodIds = requestedSlots.stream().map(SlotKey::periodId).collect(Collectors.toSet());
        List<LabOpenSlotEntity> openSlots = labOpenSlotMapper.selectActiveByLabAndWeekdaysAndPeriods(
            labId,
            weekdays.stream().toList(),
            periodIds.stream().toList()
        );
        Map<SlotKey, LabOpenSlotEntity> map = new HashMap<>();
        for (LabOpenSlotEntity slot : openSlots) {
            map.put(new SlotKey(null, slot.getWeekday(), slot.getPeriodId()), slot);
        }
        for (SlotKey key : requestedSlots) {
            if (!map.containsKey(new SlotKey(null, key.weekday(), key.periodId()))) {
                throw new BusinessException(400, "所选节次未开放，无法预约。");
            }
        }
        return map;
    }

    private void validateRoleAllowed(Map<SlotKey, LabOpenSlotEntity> openSlotMap, List<SlotKey> requestedSlots, Long currentUserId) {
        for (SlotKey key : requestedSlots) {
            LabOpenSlotEntity openSlot = openSlotMap.get(new SlotKey(null, key.weekday(), key.periodId()));
            if (!isRoleAllowedForOpenSlot(openSlot, currentUserId)) {
                throw new BusinessException(400, "当前用户角色不可预约所选节次。");
            }
        }
    }

    private boolean isRoleAllowedForOpenSlot(LabOpenSlotEntity openSlot, Long currentUserId) {
        if (openSlot == null || openSlot.getStatus() == null || openSlot.getStatus() != 1) {
            return false;
        }
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(currentUserId);
        boolean isStudent = hasRole(roleCodes, "STUDENT");
        boolean isTeacher = hasRole(roleCodes, "TEACHER");
        if (!isStudent && !isTeacher) {
            return true;
        }
        boolean allowStudent = openSlot.getAllowStudent() != null && openSlot.getAllowStudent() == 1;
        boolean allowTeacher = openSlot.getAllowTeacher() != null && openSlot.getAllowTeacher() == 1;
        if (isStudent && isTeacher) {
            return allowStudent || allowTeacher;
        }
        if (isStudent) {
            return allowStudent;
        }
        return allowTeacher;
    }

    private void validateNoMaintenance(Long labId, List<SlotKey> requestedSlots) {
        LocalDate min = requestedSlots.stream().map(SlotKey::reservationDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = requestedSlots.stream().map(SlotKey::reservationDate).max(LocalDate::compareTo).orElseThrow();
        Map<String, LabMaintenanceEntity> maintenanceMap = labMaintenanceMapper.selectByLabAndDateRange(labId, min, max).stream()
            .filter(item -> item.getStatus() != null && item.getStatus() == 1)
            .collect(Collectors.toMap(item -> key(item.getMaintenanceDate(), item.getPeriodId()), item -> item, (a, b) -> a));
        for (SlotKey key : requestedSlots) {
            LabMaintenanceEntity maintenance = maintenanceMap.get(key(key.reservationDate(), key.periodId()));
            if (maintenance != null) {
                throw new BusinessException(400, "所选节次处于维护中：" + maintenance.getReason());
            }
        }
    }

    private void validateNoReservationConflict(Long labId, List<SlotKey> requestedSlots, Long currentUserId) {
        LocalDate min = requestedSlots.stream().map(SlotKey::reservationDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = requestedSlots.stream().map(SlotKey::reservationDate).max(LocalDate::compareTo).orElseThrow();
        Map<String, ReservedSlotRow> reservedMap = labReservationSlotMapper.selectReservedSlots(labId, min, max).stream()
            .collect(Collectors.toMap(
                item -> key(item.getReservationDate(), item.getPeriodId()),
                item -> item,
                (left, right) -> choosePreferredRow(left, right, currentUserId)
            ));
        for (SlotKey key : requestedSlots) {
            ReservedSlotRow reserved = reservedMap.get(key(key.reservationDate(), key.periodId()));
            if (reserved == null) {
                continue;
            }
            if (Objects.equals(reserved.getReservationStatus(), 2) || Objects.equals(reserved.getReservationStatus(), 5)) {
                throw new BusinessException(400, "该时间段已经被预约");
            }
            if (Objects.equals(reserved.getReservationStatus(), 1) && Objects.equals(reserved.getApplicantUserId(), currentUserId)) {
                throw new BusinessException(400, "你已申请过该时段，当前状态为待审核");
            }
        }
    }

    private void insertAuditLog(Long reservationId, Long currentUserId, Integer action, String comment) {
        if (currentUserId == null) {
            return;
        }
        ReservationAuditLogEntity entity = new ReservationAuditLogEntity();
        entity.setReservationId(reservationId);
        entity.setAuditUserId(currentUserId);
        entity.setAuditAction(action);
        entity.setAuditComment(comment);
        reservationAuditLogMapper.insert(entity);
    }

    private void requireLogin(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
    }

    private void requireAdmin(Long currentUserId) {
        requireLogin(currentUserId);
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
        return date + "#" + periodId;
    }

    private record SlotKey(LocalDate reservationDate, int weekday, Long periodId) { }
}

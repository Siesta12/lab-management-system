package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.lab.LabMaintenanceCreateRequest;
import com.nlt.domain.entity.ClassPeriodEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.LabMaintenanceEntity;
import com.nlt.domain.entity.LabOpenSlotEntity;
import com.nlt.domain.vo.schedule.DailyScheduleLabItem;
import com.nlt.domain.vo.schedule.DailyScheduleResponse;
import com.nlt.domain.vo.schedule.LabMaintenanceItem;
import com.nlt.domain.vo.schedule.LabScheduleResponse;
import com.nlt.domain.vo.schedule.ReservedSlotRow;
import com.nlt.domain.vo.schedule.ReservedSlotRowWithLab;
import com.nlt.domain.vo.schedule.ScheduleCellItem;
import com.nlt.domain.vo.schedule.ScheduleDayItem;
import com.nlt.domain.vo.schedule.SchedulePeriodItem;
import com.nlt.mapper.ClassPeriodMapper;
import com.nlt.mapper.LabMaintenanceMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.LabOpenSlotMapper;
import com.nlt.mapper.LabReservationSlotMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.service.LabScheduleService;
import com.nlt.service.LabService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabScheduleServiceImpl implements LabScheduleService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LabService labService;
    private final LabMapper labMapper;
    private final ClassPeriodMapper classPeriodMapper;
    private final LabOpenSlotMapper labOpenSlotMapper;
    private final LabReservationSlotMapper labReservationSlotMapper;
    private final LabMaintenanceMapper labMaintenanceMapper;

    // For conflict checks in maintenance creation
    private final ReservationMapper reservationMapper;

    @Override
    public LabScheduleResponse getLabSchedule(Long labId, String startDate, Long currentUserId, List<String> currentRoleCodes) {
        LocalDate today = LocalDate.now();
        LocalDate start = startDate == null || startDate.isBlank() ? today : LocalDate.parse(startDate, DATE_FORMATTER);
        // Strict: schedule is always "from today for 21 days".
        if (!start.equals(today)) {
            throw new BusinessException(400, "查询起始日期必须为今天 (" + today.format(DATE_FORMATTER) + ")");
        }
        LocalDate end = start.plusDays(20);

        labService.getById(labId, currentUserId, currentRoleCodes);

        List<ClassPeriodEntity> periods = classPeriodMapper.selectActiveList();
        if (periods.isEmpty()) {
            throw new BusinessException(400, "未配置有效的上课节次");
        }
        List<Long> periodIds = periods.stream().map(ClassPeriodEntity::getId).toList();
        List<SchedulePeriodItem> periodItems = periods.stream().map(this::toPeriodItem).toList();

        List<LocalDate> dates = new ArrayList<>();
        Set<Integer> weekdays = new java.util.HashSet<>();
        for (int i = 0; i < 21; i++) {
            LocalDate d = start.plusDays(i);
            dates.add(d);
            weekdays.add(d.getDayOfWeek().getValue());
        }

        List<LabOpenSlotEntity> openSlots = labOpenSlotMapper.selectActiveByLabAndWeekdaysAndPeriods(labId,
            weekdays.stream().toList(), periodIds);
        Map<String, LabOpenSlotEntity> openMap = openSlots.stream()
            .collect(Collectors.toMap(s -> key(s.getWeekday(), s.getPeriodId()), s -> s, (a, b) -> a));

        List<LabMaintenanceEntity> maintenanceList = labMaintenanceMapper.selectByLabAndDateRange(labId, start, end).stream()
            .filter(m -> m.getStatus() != null && m.getStatus() == 1)
            .toList();
        Map<String, LabMaintenanceEntity> maintenanceMap = maintenanceList.stream()
            .collect(Collectors.toMap(m -> key(m.getMaintenanceDate(), m.getPeriodId()), m -> m, (a, b) -> a));

        List<ReservedSlotRow> reservedRows = labReservationSlotMapper.selectReservedSlots(labId, start, end);
        Map<String, ReservedSlotRow> reservedMap = reservedRows.stream()
            .collect(Collectors.toMap(r -> key(r.getReservationDate(), r.getPeriodId()), r -> r, (a, b) -> a));

        boolean isStudent = hasRole(currentRoleCodes, "STUDENT");
        boolean isTeacher = hasRole(currentRoleCodes, "TEACHER");

        List<ScheduleDayItem> days = new ArrayList<>();
        for (LocalDate date : dates) {
            int weekday = date.getDayOfWeek().getValue();
            List<ScheduleCellItem> cells = new ArrayList<>();
            for (ClassPeriodEntity period : periods) {
                LabOpenSlotEntity openSlot = openMap.get(key(weekday, period.getId()));
                boolean openAllowed = isOpenAllowed(openSlot, isStudent, isTeacher);
                if (!openAllowed) {
                    cells.add(new ScheduleCellItem(period.getId(), "CLOSED", null, null, null, null, null, "当前节次不开放"));
                    continue;
                }

                LabMaintenanceEntity maintenance = maintenanceMap.get(key(date, period.getId()));
                if (maintenance != null) {
                    cells.add(new ScheduleCellItem(period.getId(), "MAINTENANCE", null, null, null, maintenance.getId(), maintenance.getReason(), maintenance.getReason()));
                    continue;
                }

                ReservedSlotRow reserved = reservedMap.get(key(date, period.getId()));
                if (reserved != null) {
                    String status;
                    String note;
                    if (Objects.equals(reserved.getReservationStatus(), 1) && Objects.equals(reserved.getApplicantUserId(), currentUserId)) {
                        status = "PENDING_SELF";
                        note = "你已申请该时段";
                    } else if (Objects.equals(reserved.getReservationStatus(), 1)) {
                        status = "PENDING_OTHERS";
                        note = "已有他人待审核，仍可提交申请";
                    } else {
                        status = "RESERVED";
                        note = "该时段已有通过预约";
                    }
                    cells.add(new ScheduleCellItem(period.getId(), status, reserved.getReservationId(), reserved.getReservationNo(),
                        reserved.getReservationStatus(), null, null, note));
                    continue;
                }

                cells.add(new ScheduleCellItem(period.getId(), "FREE", null, null, null, null, null, "当前节次可预约"));
            }

            days.add(new ScheduleDayItem(date.format(DATE_FORMATTER), weekday, cells));
        }

        return new LabScheduleResponse(start.format(DATE_FORMATTER), end.format(DATE_FORMATTER), periodItems, days);
    }

    @Override
    public DailyScheduleResponse getDailySchedule(String date, Long currentUserId, List<String> currentRoleCodes) {
        requireAdmin(currentRoleCodes);
        LocalDate targetDate = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date, DATE_FORMATTER);
        validateWithinNext21Days(targetDate);

        List<ClassPeriodEntity> periods = classPeriodMapper.selectActiveList();
        if (periods.isEmpty()) {
            throw new BusinessException(400, "未配置有效的上课节次");
        }
        List<Long> periodIds = periods.stream().map(ClassPeriodEntity::getId).toList();
        List<SchedulePeriodItem> periodItems = periods.stream().map(this::toPeriodItem).toList();

        int weekday = targetDate.getDayOfWeek().getValue();
        List<LabEntity> labs = labMapper.selectPage(0, 500, null, null, null, null, null, null).stream()
            .filter(l -> l.getDeleted() == null || l.getDeleted() == 0)
            .toList();
        List<Long> labIds = labs.stream().map(LabEntity::getId).toList();

        Map<String, LabOpenSlotEntity> openMap = labOpenSlotMapper.selectActiveByLabIdsAndWeekdayAndPeriods(labIds, weekday, periodIds).stream()
            .collect(Collectors.toMap(s -> key(s.getLabId(), s.getPeriodId()), s -> s, (a, b) -> a));

        Map<String, ReservedSlotRowWithLab> reservedMap = labReservationSlotMapper.selectReservedSlotsForDate(targetDate).stream()
            .collect(Collectors.toMap(r -> key(r.getLabId(), r.getPeriodId()), r -> r, (a, b) -> a));

        Map<String, LabMaintenanceEntity> maintenanceMap = labMaintenanceMapper.selectActiveByDate(targetDate).stream()
            .collect(Collectors.toMap(m -> key(m.getLabId(), m.getPeriodId()), m -> m, (a, b) -> a));

        List<DailyScheduleLabItem> labItems = new ArrayList<>();
        for (LabEntity lab : labs) {
            List<ScheduleCellItem> cells = new ArrayList<>();
            for (ClassPeriodEntity period : periods) {
                LabOpenSlotEntity openSlot = openMap.get(key(lab.getId(), period.getId()));
                boolean isOpen = openSlot != null && openSlot.getStatus() != null && openSlot.getStatus() == 1;
                if (!isOpen) {
                    cells.add(new ScheduleCellItem(period.getId(), "CLOSED", null, null, null, null, null, "当前节次不开放"));
                    continue;
                }

                LabMaintenanceEntity maintenance = maintenanceMap.get(key(lab.getId(), period.getId()));
                if (maintenance != null) {
                    cells.add(new ScheduleCellItem(period.getId(), "MAINTENANCE", null, null, null, maintenance.getId(), maintenance.getReason(), maintenance.getReason()));
                    continue;
                }

                ReservedSlotRowWithLab reserved = reservedMap.get(key(lab.getId(), period.getId()));
                if (reserved != null) {
                    String status = Objects.equals(reserved.getReservationStatus(), 1) ? "PENDING" : "RESERVED";
                    cells.add(new ScheduleCellItem(period.getId(), status, reserved.getReservationId(), reserved.getReservationNo(),
                        reserved.getReservationStatus(), null, null, Objects.equals(reserved.getReservationStatus(), 1) ? "当前时段存在待审核预约" : "当前时段已占用"));
                    continue;
                }

                cells.add(new ScheduleCellItem(period.getId(), "FREE", null, null, null, null, null, "当前节次可预约"));
            }
            labItems.add(new DailyScheduleLabItem(lab.getId(), lab.getLabName(), cells));
        }

        return new DailyScheduleResponse(targetDate.format(DATE_FORMATTER), weekday, periodItems, labItems);
    }

    @Override
    public List<LabMaintenanceItem> listLabMaintenance(Long labId, Long currentUserId, List<String> currentRoleCodes) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(20);
        labService.getById(labId, currentUserId, currentRoleCodes);

        Map<Long, ClassPeriodEntity> periodMap = classPeriodMapper.selectActiveList().stream()
            .collect(Collectors.toMap(ClassPeriodEntity::getId, p -> p, (a, b) -> a));

        return labMaintenanceMapper.selectByLabAndDateRange(labId, today, end).stream()
            .map(m -> new LabMaintenanceItem(
                m.getId(),
                m.getLabId(),
                m.getMaintenanceDate().format(DATE_FORMATTER),
                m.getMaintenanceDate().getDayOfWeek().getValue(),
                m.getPeriodId(),
                periodMap.get(m.getPeriodId()) == null ? null : periodMap.get(m.getPeriodId()).getPeriodName(),
                m.getReason(),
                m.getStatus(),
                m.getOperatorUserId()
            ))
            .toList();
    }

    @Transactional
    @Override
    public List<LabMaintenanceItem> createMaintenance(Long labId, LabMaintenanceCreateRequest request, Long operatorUserId,
        List<String> currentRoleCodes) {
        requireAdmin(currentRoleCodes);
        if (operatorUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        labService.getById(labId, operatorUserId, currentRoleCodes);

        LocalDate maintenanceDate = LocalDate.parse(request.getMaintenanceDate(), DATE_FORMATTER);
        validateWithinNext21Days(maintenanceDate);
        if (request.getPeriodIds() == null || request.getPeriodIds().isEmpty()) {
            throw new BusinessException(400, "节次ID不能为空");
        }

        // Disallow maintenance if there is any effective reservation occupying the slot.
        // We treat reservation status (1,2,5) as effective; slot_status=1 indicates occupied.
        Map<String, ReservedSlotRow> reserved = new HashMap<>();
        for (ReservedSlotRow row : labReservationSlotMapper.selectReservedSlots(labId, maintenanceDate, maintenanceDate)) {
            reserved.put(key(row.getReservationDate(), row.getPeriodId()), row);
        }
        for (Long periodId : request.getPeriodIds()) {
            if (reserved.containsKey(key(maintenanceDate, periodId))) {
                throw new BusinessException(400, "该时段已有预约，无法设置维护（节次ID=" + periodId + "）");
            }
        }

        List<LabMaintenanceEntity> list = request.getPeriodIds().stream().distinct().map(periodId -> {
            LabMaintenanceEntity entity = new LabMaintenanceEntity();
            entity.setLabId(labId);
            entity.setMaintenanceDate(maintenanceDate);
            entity.setPeriodId(periodId);
            entity.setReason(request.getReason());
            entity.setStatus(1);
            entity.setOperatorUserId(operatorUserId);
            return entity;
        }).toList();

        labMaintenanceMapper.upsertBatch(list);
        return listLabMaintenance(labId, operatorUserId, currentRoleCodes);
    }

    @Transactional
    @Override
    public void cancelMaintenance(Long labId, Long maintenanceId, Long operatorUserId, List<String> currentRoleCodes) {
        requireAdmin(currentRoleCodes);
        if (operatorUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        labService.getById(labId, operatorUserId, currentRoleCodes);
        LabMaintenanceEntity entity = labMaintenanceMapper.selectById(maintenanceId);
        if (entity == null || !Objects.equals(entity.getLabId(), labId)) {
            throw new BusinessException(404, "维护记录不存在或不属于当前实验室");
        }
        labMaintenanceMapper.cancel(maintenanceId, operatorUserId);
    }

    private void validateWithinNext21Days(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(20);
        if (date.isBefore(today) || date.isAfter(last)) {
            throw new BusinessException(400, "日期超出允许范围（今天起21天内）: " + date.format(DATE_FORMATTER));
        }
    }

    private SchedulePeriodItem toPeriodItem(ClassPeriodEntity entity) {
        return new SchedulePeriodItem(
            entity.getId(),
            entity.getPeriodNo(),
            entity.getPeriodName(),
            formatTime(entity.getStartTime()),
            formatTime(entity.getEndTime())
        );
    }

    private String formatTime(LocalTime value) {
        return value == null ? null : value.format(TIME_FORMATTER);
    }

    private void requireAdmin(List<String> roleCodes) {
        if (!hasRole(roleCodes, "ADMIN")) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    private boolean hasRole(List<String> roleCodes, String roleCode) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(code ->
            roleCode.equalsIgnoreCase(code) || ("ROLE_" + roleCode).equalsIgnoreCase(code));
    }

    private boolean isOpenAllowed(LabOpenSlotEntity slot, boolean isStudent, boolean isTeacher) {
        if (slot == null || slot.getStatus() == null || slot.getStatus() != 1) {
            return false;
        }
        if (!isStudent && !isTeacher) {
            return true;
        }
        boolean allowStudent = slot.getAllowStudent() != null && slot.getAllowStudent() == 1;
        boolean allowTeacher = slot.getAllowTeacher() != null && slot.getAllowTeacher() == 1;
        if (isStudent && isTeacher) {
            return allowStudent || allowTeacher;
        }
        if (isStudent) {
            return allowStudent;
        }
        return allowTeacher;
    }

    private String key(Integer weekday, Long periodId) {
        return weekday + "#" + periodId;
    }

    private String key(LocalDate date, Long periodId) {
        return date.toString() + "#" + periodId;
    }

    private String key(Long labId, Long periodId) {
        return labId + "#" + periodId;
    }
}


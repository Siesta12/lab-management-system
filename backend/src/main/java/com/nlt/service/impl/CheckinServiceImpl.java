package com.nlt.service.impl;

import com.nlt.common.constant.ReservationCheckConstants;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.checkin.CheckinSubmitRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import com.nlt.domain.entity.ViolationRecordEntity;
import com.nlt.domain.vo.checkin.CheckinResultVo;
import com.nlt.domain.vo.reservation.ReservationCheckCandidateVo;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.ReservationAuditLogMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.mapper.ViolationMapper;
import com.nlt.service.CheckinService;
import com.nlt.service.DepartmentService;
import com.nlt.util.GeoDistanceUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReservationMapper reservationMapper;
    private final ReservationAuditLogMapper reservationAuditLogMapper;
    private final LabMapper labMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ViolationMapper violationMapper;
    private final DepartmentService departmentService;

    @Override
    @Transactional
    public CheckinResultVo submit(CheckinSubmitRequest request, LabEntity lab, Long currentUserId) {
        requireLogin(currentUserId);
        if (request == null) {
            throw new BusinessException(400, "\u7B7E\u5230\u8BF7\u6C42\u4E0D\u80FD\u4E3A\u7A7A");
        }
        if (lab == null) {
            throw new BusinessException(404, "\u672A\u627E\u5230\u5B9E\u9A8C\u5BA4\u4FE1\u606F");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException(400, "\u5F53\u524D\u4F4D\u7F6E\u7F3A\u5C11\u7ECF\u7EAC\u5EA6\u4FE1\u606F\uFF0C\u65E0\u6CD5\u7B7E\u5230");
        }
        if (lab.getLatitude() == null || lab.getLongitude() == null) {
            throw new BusinessException(400, "\u5B9E\u9A8C\u5BA4\u672A\u914D\u7F6E\u5B9A\u4F4D\u4FE1\u606F\uFF0C\u6682\u65E0\u6CD5\u7B7E\u5230");
        }

        LocalDateTime now = LocalDateTime.now();
        ReservationCheckCandidateVo candidate = resolveCheckInCandidate(lab.getId(), now);

        double distanceMeters = GeoDistanceUtils.distanceMeters(
            request.getLatitude(),
            request.getLongitude(),
            lab.getLatitude(),
            lab.getLongitude()
        );
        if (distanceMeters > ReservationCheckConstants.MAX_CHECK_IN_DISTANCE_METERS) {
            throw new BusinessException(400, "\u5F53\u524D\u4F4D\u7F6E\u4E0D\u5728\u5B9E\u9A8C\u5BA4\u9644\u8FD1\uFF0C\u7B7E\u5230\u5931\u8D25");
        }

        LocalDateTime startDateTime = buildStartDateTime(candidate);
        boolean late = now.isAfter(startDateTime);
        reservationMapper.checkInAt(candidate.getReservationId(), now);

        int scoreChange = 0;
        String auditComment = "\u6B63\u5E38\u7B7E\u5230";
        Long actualUserId = candidate.getApplicantUserId();
        if (late) {
            scoreChange = ReservationCheckConstants.LATE_SCORE_DEDUCTION;
            auditComment = "\u8FDF\u5230\u7B7E\u5230";
            applyViolationPenalty(
                actualUserId,
                candidate.getReservationId(),
                ReservationCheckConstants.VIOLATION_TYPE_LATE,
                scoreChange,
                "\u9884\u7EA6\u7B7E\u5230\u8FDF\u5230"
            );
        }

        insertAuditLog(
            candidate.getReservationId(),
            currentUserId != null ? currentUserId : actualUserId,
            ReservationCheckConstants.AUDIT_ACTION_CHECK_IN,
            auditComment
        );
        return buildResult(
            "CHECK_IN",
            candidate,
            now,
            late,
            scoreChange,
            Math.round(distanceMeters),
            late ? "\u8FDF\u5230\u7B7E\u5230\u6210\u529F" : "\u7B7E\u5230\u6210\u529F"
        );
    }

    @Override
    @Transactional
    public int handleNoShowReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<ReservationCheckCandidateVo> candidates = reservationMapper.selectNoShowCandidates(now.toLocalDate());
        int handledCount = 0;
        for (ReservationCheckCandidateVo candidate : candidates) {
            if (candidate.getCheckInTime() != null) {
                continue;
            }
            LocalDateTime lateDeadline = buildStartDateTime(candidate)
                .plusMinutes(ReservationCheckConstants.CHECK_IN_LATE_MINUTES);
            if (!now.isAfter(lateDeadline)) {
                continue;
            }
            if (violationMapper.existsByReservationAndType(
                candidate.getReservationId(),
                ReservationCheckConstants.VIOLATION_TYPE_NO_SHOW
            )) {
                continue;
            }
            applyViolationPenalty(
                candidate.getApplicantUserId(),
                candidate.getReservationId(),
                ReservationCheckConstants.VIOLATION_TYPE_NO_SHOW,
                ReservationCheckConstants.NO_SHOW_SCORE_DEDUCTION,
                "\u8D85\u8FC7\u7B7E\u5230\u7A97\u53E3\u672A\u7B7E\u5230\uFF0C\u8BB0\u4E3A\u7CDE\u7EA6"
            );
            insertAuditLog(
                candidate.getReservationId(),
                candidate.getApplicantUserId(),
                ReservationCheckConstants.AUDIT_ACTION_CHECK_OUT,
                "\u8D85\u8FC7\u7B7E\u5230\u7A97\u53E3\u672A\u7B7E\u5230\uFF0C\u8BB0\u4E3A\u7CDE\u7EA6"
            );
            handledCount++;
        }
        return handledCount;
    }

    @Override
    @Transactional
    public int handleAutoCompleteReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<ReservationCheckCandidateVo> candidates = reservationMapper.selectCompletedCandidates(now);
        int handledCount = 0;
        for (ReservationCheckCandidateVo candidate : candidates) {
            if (reservationMapper.markCompleted(candidate.getReservationId()) <= 0) {
                continue;
            }
            insertAuditLog(
                candidate.getReservationId(),
                candidate.getApplicantUserId(),
                ReservationCheckConstants.AUDIT_ACTION_CHECK_OUT,
                "\u5B9E\u9A8C\u5DF2\u5B8C\u6210"
            );
            handledCount++;
        }
        return handledCount;
    }

    /**
     * 按实验室和日期查找可签到预约，只允许学生进入签到链路。
     * 教师预约不需要签到，也不会进入爽约与信誉分处理。
     */
    private ReservationCheckCandidateVo resolveCheckInCandidate(Long labId, LocalDateTime now) {
        List<ReservationCheckCandidateVo> reservations =
            reservationMapper.selectCheckInCandidatesByLab(labId, now.toLocalDate());
        if (reservations == null || reservations.isEmpty()) {
            throw new BusinessException(404, "\u5F53\u524D\u4E0D\u5B58\u5728\u53EF\u7B7E\u5230\u9884\u7EA6");
        }

        Map<Long, Boolean> teacherRoleCache = new HashMap<>();
        List<ReservationCheckCandidateVo> studentReservations = new ArrayList<>();
        boolean hasTeacherReservation = false;
        for (ReservationCheckCandidateVo candidate : reservations) {
            if (isTeacherApplicant(candidate.getApplicantUserId(), teacherRoleCache)) {
                hasTeacherReservation = true;
                continue;
            }
            studentReservations.add(candidate);
        }
        if (studentReservations.isEmpty()) {
            if (hasTeacherReservation) {
                throw new BusinessException(400, "\u6559\u5E08\u9884\u7EA6\u65E0\u9700\u7B7E\u5230");
            }
            throw new BusinessException(404, "\u5F53\u524D\u4E0D\u5B58\u5728\u53EF\u7B7E\u5230\u9884\u7EA6");
        }

        List<ReservationCheckCandidateVo> withinWindow = new ArrayList<>();
        ReservationCheckCandidateVo nearestFuture = null;
        ReservationCheckCandidateVo nearestExpired = null;

        for (ReservationCheckCandidateVo candidate : studentReservations) {
            LocalDateTime startDateTime = buildStartDateTime(candidate);
            LocalDateTime earlyTime = startDateTime.minusMinutes(ReservationCheckConstants.CHECK_IN_EARLY_MINUTES);
            LocalDateTime lateTime = startDateTime.plusMinutes(ReservationCheckConstants.CHECK_IN_LATE_MINUTES);

            if (now.isBefore(earlyTime)) {
                if (nearestFuture == null || buildStartDateTime(nearestFuture).isAfter(startDateTime)) {
                    nearestFuture = candidate;
                }
                continue;
            }
            if (now.isAfter(lateTime)) {
                if (nearestExpired == null || buildStartDateTime(nearestExpired).isBefore(startDateTime)) {
                    nearestExpired = candidate;
                }
                continue;
            }
            withinWindow.add(candidate);
        }

        if (withinWindow.size() > 1) {
            throw new BusinessException(400, "\u5F53\u524D\u5B58\u5728\u591A\u6761\u53EF\u7B7E\u5230\u9884\u7EA6\uFF0C\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458");
        }
        if (withinWindow.size() == 1) {
            ReservationCheckCandidateVo candidate = withinWindow.get(0);
            if (candidate.getCheckInTime() != null) {
                throw new BusinessException(400, "\u5DF2\u7B7E\u5230\uFF0C\u8BF7\u52FF\u91CD\u590D\u64CD\u4F5C");
            }
            return candidate;
        }
        if (nearestFuture != null) {
            throw new BusinessException(400, "\u672A\u5230\u7B7E\u5230\u65F6\u95F4");
        }
        if (nearestExpired != null) {
            throw new BusinessException(400, "\u5DF2\u8D85\u8FC7\u7B7E\u5230\u65F6\u95F4\uFF0C\u65E0\u6CD5\u7B7E\u5230");
        }
        throw new BusinessException(404, "\u5F53\u524D\u4E0D\u5B58\u5728\u53EF\u7B7E\u5230\u9884\u7EA6");
    }

    private boolean isTeacherApplicant(Long userId, Map<Long, Boolean> teacherRoleCache) {
        if (userId == null) {
            return false;
        }
        return teacherRoleCache.computeIfAbsent(userId, this::hasTeacherRole);
    }

    private boolean hasTeacherRole(Long userId) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(code ->
            "TEACHER".equalsIgnoreCase(code) || "ROLE_TEACHER".equalsIgnoreCase(code));
    }

    private LocalDateTime buildStartDateTime(ReservationCheckCandidateVo candidate) {
        return LocalDateTime.of(candidate.getReservationDate(), candidate.getStartTime());
    }

    private void applyViolationPenalty(Long userId, Long reservationId, Integer violationType, int scoreDelta, String remark) {
        if (violationMapper.existsByReservationAndType(reservationId, violationType)) {
            return;
        }
        userMapper.adjustCreditAndViolation(userId, scoreDelta, 1);
        ViolationRecordEntity entity = new ViolationRecordEntity();
        entity.setUserId(userId);
        entity.setReservationId(reservationId);
        entity.setViolationType(violationType);
        entity.setScoreChange(scoreDelta);
        entity.setRemark(remark);
        violationMapper.insert(entity);
    }

    private void insertAuditLog(Long reservationId, Long auditUserId, Integer action, String comment) {
        if (auditUserId == null) {
            return;
        }
        ReservationAuditLogEntity entity = new ReservationAuditLogEntity();
        entity.setReservationId(reservationId);
        entity.setAuditUserId(auditUserId);
        entity.setAuditAction(action);
        entity.setAuditComment(comment);
        reservationAuditLogMapper.insert(entity);
    }

    private void requireLogin(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "\u8BF7\u5148\u767B\u5F55\u7CFB\u7EDF\u540E\u518D\u7B7E\u5230");
        }
    }

    private CheckinResultVo buildResult(
        String action,
        ReservationCheckCandidateVo candidate,
        LocalDateTime checkTime,
        boolean late,
        int scoreChange,
        long distanceMeters,
        String message
    ) {
        return new CheckinResultVo(
            action,
            candidate.getReservationId(),
            candidate.getReservationNo(),
            candidate.getLabId(),
            candidate.getLabName(),
            candidate.getReservationDate().format(DATE_FORMATTER),
            candidate.getPeriodName(),
            formatDateTime(checkTime),
            late,
            scoreChange,
            distanceMeters,
            message
        );
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }
}

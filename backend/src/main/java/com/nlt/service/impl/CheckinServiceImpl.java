package com.nlt.service.impl;

import com.nlt.common.constant.ReservationCheckConstants;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.checkin.CheckinSubmitRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.UserEntity;
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
            throw new BusinessException(400, "签到请求不能为空");
        }
        if (lab == null) {
            throw new BusinessException(404, "未找到实验室信息");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException(400, "当前位置缺少经纬度信息，无法签到");
        }
        if (lab.getLatitude() == null || lab.getLongitude() == null) {
            throw new BusinessException(400, "实验室未配置定位信息，暂无法签到");
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
            throw new BusinessException(400, "当前位置不在实验室附近，签到失败");
        }

        LocalDateTime startDateTime = buildStartDateTime(candidate);
        boolean late = now.isAfter(startDateTime);
        reservationMapper.checkInAt(candidate.getReservationId(), now);

        int scoreChange = 0;
        String auditComment = "正常签到";
        Long actualUserId = candidate.getApplicantUserId();
        if (late) {
            scoreChange = ReservationCheckConstants.LATE_SCORE_DEDUCTION;
            auditComment = "迟到签到";
            applyViolationPenalty(
                actualUserId,
                candidate.getReservationId(),
                ReservationCheckConstants.VIOLATION_TYPE_LATE,
                scoreChange,
                "预约签到迟到"
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
            late ? "迟到签到成功" : "签到成功"
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
                "超过签到窗口未签到，记为爽约"
            );
            insertAuditLog(
                candidate.getReservationId(),
                candidate.getApplicantUserId(),
                ReservationCheckConstants.AUDIT_ACTION_CHECK_OUT,
                "超过签到窗口未签到，记为爽约"
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
                "实验已完成"
            );
            applyNormalCompletionReward(candidate);
            handledCount++;
        }
        return handledCount;
    }

    private ReservationCheckCandidateVo resolveCheckInCandidate(Long labId, LocalDateTime now) {
        List<ReservationCheckCandidateVo> reservations =
            reservationMapper.selectCheckInCandidatesByLab(labId, now.toLocalDate());
        if (reservations == null || reservations.isEmpty()) {
            throw new BusinessException(404, "当前不存在可签到预约");
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
                throw new BusinessException(400, "教师预约无需签到");
            }
            throw new BusinessException(404, "当前不存在可签到预约");
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
            throw new BusinessException(400, "当前存在多条可签到预约，请联系管理员");
        }
        if (withinWindow.size() == 1) {
            ReservationCheckCandidateVo candidate = withinWindow.get(0);
            if (candidate.getCheckInTime() != null) {
                throw new BusinessException(400, "已签到，请勿重复操作");
            }
            return candidate;
        }
        if (nearestFuture != null) {
            throw new BusinessException(400, "未到签到时间");
        }
        if (nearestExpired != null) {
            throw new BusinessException(400, "已超过签到时间，无法签到");
        }
        throw new BusinessException(404, "当前不存在可签到预约");
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
        userMapper.resetNormalReservationStreak(userId);
        ViolationRecordEntity entity = new ViolationRecordEntity();
        entity.setUserId(userId);
        entity.setReservationId(reservationId);
        entity.setViolationType(violationType);
        entity.setScoreChange(scoreDelta);
        entity.setRemark(remark);
        violationMapper.insert(entity);
    }

    private void applyNormalCompletionReward(ReservationCheckCandidateVo candidate) {
        if (candidate == null || candidate.getApplicantUserId() == null || candidate.getCheckInTime() == null) {
            return;
        }
        LocalDateTime startDateTime = buildStartDateTime(candidate);
        if (candidate.getCheckInTime().isAfter(startDateTime)) {
            return;
        }
        if (violationMapper.existsByReservationId(candidate.getReservationId())) {
            return;
        }

        Long userId = candidate.getApplicantUserId();
        userMapper.adjustCreditAndViolation(userId, 2, 0);

        UserEntity user = userMapper.selectById(userId);
        int streak = user == null || user.getNormalReservationStreak() == null ? 0 : user.getNormalReservationStreak();
        streak++;
        if (streak >= 3) {
            userMapper.adjustCreditAndViolation(userId, 5, 0);
            userMapper.resetNormalReservationStreak(userId);
            return;
        }
        userMapper.updateNormalReservationStreak(userId, streak);
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
            throw new BusinessException(401, "请先登录系统后再签到");
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

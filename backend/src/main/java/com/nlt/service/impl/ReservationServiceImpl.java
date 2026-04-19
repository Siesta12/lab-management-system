package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.reservation.ConflictCheckRequest;
import com.nlt.domain.dto.reservation.RecommendationRequest;
import com.nlt.domain.dto.reservation.ReservationAuditRequest;
import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import com.nlt.domain.entity.ReservationEntity;
import com.nlt.domain.vo.reservation.ConflictCheckData;
import com.nlt.domain.vo.reservation.LabRecommendationItem;
import com.nlt.domain.vo.reservation.TimeRecommendationItem;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.ReservationAuditLogMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.service.ReservationService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][ ]HH:mm:ss");

    private final ReservationMapper reservationMapper;

    private final ReservationAuditLogMapper reservationAuditLogMapper;

    private final LabMapper labMapper;

    /**
     * 查询预约信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param reservationNo 预约编号
     * @param labId 实验室ID
     * @param applicantUserId 申请人用户ID
     * @param approverUserId 审批人用户ID
     * @param status 状态值
     * @param reservationDate 预约日期
     * @return 分页数据
     */
    @Override
    public PageData<ReservationEntity> page(int pageNum, int pageSize, String reservationNo, Long labId,
    Long applicantUserId, Long approverUserId, Integer status,
    String reservationDate) {
        int offset = (pageNum - 1) * pageSize;
        LocalDate date = reservationDate == null || reservationDate.isBlank() ? null : LocalDate.parse(reservationDate, DATE_FORMATTER);
        return new PageData<>(
        reservationMapper.selectPage(offset, pageSize, reservationNo, labId, applicantUserId, approverUserId, status, date),
        reservationMapper.countPage(reservationNo, labId, applicantUserId, approverUserId, status, date),
        pageNum,
        pageSize
        );
    }

    /**
     * 查询当前用户预约信息列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    @Override
    public PageData<ReservationEntity> mine(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(reservationMapper.selectMine(userId, offset, pageSize), reservationMapper.countMine(userId), pageNum, pageSize);
    }

    /**
     * 查询待审批预约信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    @Override
    public PageData<ReservationEntity> pendingAudit(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(reservationMapper.selectPendingAudit(offset, pageSize), reservationMapper.countPendingAudit(), pageNum, pageSize);
    }

    /**
     * 查询预约信息
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public ReservationEntity getById(Long id) {
        ReservationEntity entity = reservationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        return entity;
    }

    /**
     * 新增预约信息
     * @param request 请求参数
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    @Transactional
    @Override
    public ReservationEntity create(ReservationCreateRequest request, Long currentUserId) {
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
        int conflictCount = reservationMapper.countConflict(request.getLabId(), startTime, endTime);
        if (conflictCount > 0) {
            throw new BusinessException(400, "预约时间与已有预约冲突");
        }
        ReservationEntity entity = new ReservationEntity();
        entity.setReservationNo("RES" + System.currentTimeMillis());
        entity.setLabId(request.getLabId());
        entity.setApplicantUserId(currentUserId);
        entity.setReservationType(request.getReservationType() == null ? 3 : request.getReservationType());
        entity.setPriorityLevel(request.getPriorityLevel() == null ? 3 : request.getPriorityLevel());
        entity.setReservationDate(LocalDate.parse(request.getReservationDate(), DATE_FORMATTER));
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setUsagePurpose(request.getUsagePurpose());
        entity.setCourseOrProjectName(request.getCourseOrProjectName());
        entity.setParticipantCount(request.getParticipantCount() == null ? 1 : request.getParticipantCount());
        entity.setContactPhone(request.getContactPhone());
        entity.setStatus(1);
        reservationMapper.insert(entity);
        insertAuditLog(entity.getId(), currentUserId, 1, "提交预约");
        return getById(entity.getId());
    }

    /**
     * 检查预约冲突情况
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public ConflictCheckData conflictCheck(ConflictCheckRequest request) {
        int count = reservationMapper.countConflict(request.getLabId(), parseDateTime(request.getStartTime()), parseDateTime(request.getEndTime()));
        return new ConflictCheckData(count > 0, count, count > 0 ? "该时间段已被占用" : "该时间段可以预约");
    }

    /**
     * 推荐可用时间段
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<TimeRecommendationItem> recommendTime(RecommendationRequest request) {
        LocalDate date = LocalDate.parse(request.getReservationDate(), DATE_FORMATTER);
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        long minutes = Duration.between(startTime, endTime).toMinutes();
        List<TimeRecommendationItem> result = new ArrayList<>();
        List<ReservationEntity> reservations = reservationMapper.selectByLabAndDate(request.getLabId(), date);
        for (int i = 1; i <= 3; i++) {
            LocalDateTime newStart = startTime.plusMinutes(minutes * i);
            LocalDateTime newEnd = newStart.plusMinutes(minutes);
            if (reservationMapper.countConflict(request.getLabId(), newStart, newEnd) == 0) {
                result.add(new TimeRecommendationItem(newStart.toString(), newEnd.toString(), "后续可用时段"));
            }
        }
        for (int i = 1; i <= 3; i++) {
            LocalDateTime newEnd = startTime.minusMinutes(minutes * i);
            LocalDateTime newStart = newEnd.minusMinutes(minutes);
            if (reservationMapper.countConflict(request.getLabId(), newStart, newEnd) == 0) {
                result.add(new TimeRecommendationItem(newStart.toString(), newEnd.toString(), "前序可用时段"));
            }
        }
        if (result.isEmpty() && reservations.isEmpty()) {
            result.add(new TimeRecommendationItem(startTime.toString(), endTime.toString(), "当天暂无预约记录"));
        }
        return result;
    }

    /**
     * 推荐可用实验室
     * @param request 请求参数
     * @return 数据列表
     */
    @Override
    public List<LabRecommendationItem> recommendLabs(RecommendationRequest request) {
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        return labMapper.selectRecommendationCandidates(request.getLabId(), request.getParticipantCount()).stream()
        .filter(lab -> reservationMapper.countConflict(lab.getId(), startTime, endTime) == 0)
        .limit(5)
        .map(lab -> toLabRecommendation(lab, startTime, endTime))
        .toList();
    }

    /**
     * 审批预约信息
     * @param id 主键ID
     * @param request 请求参数
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    @Transactional
    @Override
    public ReservationEntity audit(Long id, ReservationAuditRequest request, Long currentUserId) {
        ReservationEntity entity = getById(id);
        if (request.getAuditAction() == null || (request.getAuditAction() != 2 && request.getAuditAction() != 3)) {
            throw new BusinessException(400, "审核操作仅支持通过或驳回");
        }
        entity.setApproverUserId(currentUserId);
        entity.setStatus(request.getAuditAction() == 2 ? 2 : 3);
        entity.setRejectReason(request.getRejectReason());
        reservationMapper.updateAuditResult(entity);
        insertAuditLog(id, currentUserId, request.getAuditAction(), request.getAuditComment());
        return getById(id);
    }

    /**
     * 取消预约信息
     * @param id 主键ID
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    @Transactional
    @Override
    public ReservationEntity cancel(Long id, Long currentUserId) {
        getById(id);
        reservationMapper.cancel(id);
        insertAuditLog(id, currentUserId, 4, "取消预约");
        return getById(id);
    }

    /**
     * 预约信息签到
     * @param id 主键ID
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    @Transactional
    @Override
    public ReservationEntity checkIn(Long id, Long currentUserId) {
        getById(id);
        reservationMapper.checkIn(id);
        insertAuditLog(id, currentUserId, 5, "签到");
        return getById(id);
    }

    /**
     * 预约信息签退
     * @param id 主键ID
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    @Transactional
    @Override
    public ReservationEntity checkOut(Long id, Long currentUserId) {
        getById(id);
        reservationMapper.checkOut(id);
        insertAuditLog(id, currentUserId, 5, "签退并完成");
        return getById(id);
    }

    /**
     * 新增预约信息
     * @param reservationId 预约ID
     * @param currentUserId currentUserID
     * @param action 参数
     * @param comment 参数
     */
    private void insertAuditLog(Long reservationId, Long currentUserId, Integer action, String comment) {
        ReservationAuditLogEntity logEntity = new ReservationAuditLogEntity();
        logEntity.setReservationId(reservationId);
        logEntity.setAuditUserId(currentUserId);
        logEntity.setAuditAction(action);
        logEntity.setAuditComment(comment);
        reservationAuditLogMapper.insert(logEntity);
    }

    /**
     * 处理预约信息
     * @param value 参数
     * @return 处理结果
     */
    private LocalDateTime parseDateTime(String value) {
        String normalized = value.replace("T", " ");
        return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 转换预约信息
     * @param lab 参数
     * @param startTime 时间参数
     * @param endTime 时间参数
     * @return 处理结果
     */
    private LabRecommendationItem toLabRecommendation(LabEntity lab, LocalDateTime startTime, LocalDateTime endTime) {
        return new LabRecommendationItem(
        lab.getId(),
        lab.getLabCode(),
        lab.getLabName(),
        lab.getBuildingName(),
        lab.getRoomNo(),
        startTime.toString(),
        endTime.toString(),
        "可预约的备选实验室"
        );
    }

}

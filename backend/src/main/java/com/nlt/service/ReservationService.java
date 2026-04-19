package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.reservation.ConflictCheckRequest;
import com.nlt.domain.dto.reservation.RecommendationRequest;
import com.nlt.domain.dto.reservation.ReservationAuditRequest;
import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.entity.ReservationEntity;
import com.nlt.domain.vo.reservation.ConflictCheckData;
import com.nlt.domain.vo.reservation.LabRecommendationItem;
import com.nlt.domain.vo.reservation.TimeRecommendationItem;
import java.util.List;

public interface ReservationService {

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
    public PageData<ReservationEntity> page(int pageNum, int pageSize, String reservationNo, Long labId,
    Long applicantUserId, Long approverUserId, Integer status,
    String reservationDate);

    /**
     * 查询当前用户预约信息列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    PageData<ReservationEntity> mine(Long userId, int pageNum, int pageSize);

    /**
     * 查询待审批预约信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    PageData<ReservationEntity> pendingAudit(int pageNum, int pageSize);

    /**
     * 查询预约信息
     * @param id 主键ID
     * @return 处理结果
     */
    ReservationEntity getById(Long id);

    /**
     * 新增预约信息
     * @param request 请求参数
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    ReservationEntity create(ReservationCreateRequest request, Long currentUserId);

    /**
     * 检查预约冲突情况
     * @param request 请求参数
     * @return 处理结果
     */
    ConflictCheckData conflictCheck(ConflictCheckRequest request);

    /**
     * 推荐可用时间段
     * @param request 请求参数
     * @return 数据列表
     */
    List<TimeRecommendationItem> recommendTime(RecommendationRequest request);

    /**
     * 推荐可用实验室
     * @param request 请求参数
     * @return 数据列表
     */
    List<LabRecommendationItem> recommendLabs(RecommendationRequest request);

    /**
     * 审批预约信息
     * @param id 主键ID
     * @param request 请求参数
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    ReservationEntity audit(Long id, ReservationAuditRequest request, Long currentUserId);

    /**
     * 取消预约信息
     * @param id 主键ID
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    ReservationEntity cancel(Long id, Long currentUserId);

    /**
     * 预约信息签到
     * @param id 主键ID
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    ReservationEntity checkIn(Long id, Long currentUserId);

    /**
     * 预约信息签退
     * @param id 主键ID
     * @param currentUserId currentUserID
     * @return 处理结果
     */
    ReservationEntity checkOut(Long id, Long currentUserId);

}

package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.reservation.ConflictCheckRequest;
import com.nlt.domain.dto.reservation.RecommendationRequest;
import com.nlt.domain.dto.reservation.ReservationAuditRequest;
import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.entity.ReservationEntity;
import com.nlt.domain.vo.reservation.ConflictCheckData;
import com.nlt.domain.vo.reservation.LabRecommendationItem;
import com.nlt.domain.vo.reservation.TimeRecommendationItem;
import com.nlt.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final TokenService tokenService;

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
     * @return 响应结果
     */
    @GetMapping
    public ApiResponse<PageData<ReservationEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String reservationNo,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) Long applicantUserId,
        @RequestParam(required = false) Long approverUserId,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String reservationDate) {
        return ApiResponse.success(reservationService.page(pageNum, pageSize, reservationNo, labId, applicantUserId,
        approverUserId, status, reservationDate));
    }

    /**
     * 新增预约信息
     * @param request 请求参数
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<ReservationEntity> create(@Valid @RequestBody ReservationCreateRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.create(request, tokenService.getCurrentUserId(servletRequest)));
    }

    /**
     * 查询当前用户预约信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @GetMapping("/mine")
    public ApiResponse<PageData<ReservationEntity>> mine(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.mine(tokenService.getCurrentUserId(servletRequest), pageNum, pageSize));
    }

    /**
     * 查询待审批预约信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 响应结果
     */
    @GetMapping("/pending-audit")
    public ApiResponse<PageData<ReservationEntity>> pendingAudit(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(reservationService.pendingAudit(pageNum, pageSize));
    }

    /**
     * 检查预约冲突情况
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/conflict-check")
    public ApiResponse<ConflictCheckData> conflictCheck(@Valid @RequestBody ConflictCheckRequest request) {
        return ApiResponse.success(reservationService.conflictCheck(request));
    }

    /**
     * 推荐可用时间段
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/recommend-time")
    public ApiResponse<List<TimeRecommendationItem>> recommendTime(@Valid @RequestBody RecommendationRequest request) {
        return ApiResponse.success(reservationService.recommendTime(request));
    }

    /**
     * 推荐可用实验室
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/recommend-labs")
    public ApiResponse<List<LabRecommendationItem>> recommendLabs(@Valid @RequestBody RecommendationRequest request) {
        return ApiResponse.success(reservationService.recommendLabs(request));
    }

    /**
     * 查询预约信息
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<ReservationEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(reservationService.getById(id));
    }

    /**
     * 审批预约信息
     * @param id 主键ID
     * @param request 请求参数
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @PostMapping("/{id}/audit")
    public ApiResponse<ReservationEntity> audit(@PathVariable Long id,
        @Valid @RequestBody ReservationAuditRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.audit(id, request, tokenService.getCurrentUserId(servletRequest)));
    }

    /**
     * 取消预约信息
     * @param id 主键ID
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ReservationEntity> cancel(@PathVariable Long id,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.cancel(id, tokenService.getCurrentUserId(servletRequest)));
    }

    /**
     * 预约信息签到
     * @param id 主键ID
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @PostMapping("/{id}/check-in")
    public ApiResponse<ReservationEntity> checkIn(@PathVariable Long id,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.checkIn(id, tokenService.getCurrentUserId(servletRequest)));
    }

    /**
     * 预约信息签退
     * @param id 主键ID
     * @param servletRequest HTTP请求对象
     * @return 响应结果
     */
    @PostMapping("/{id}/check-out")
    public ApiResponse<ReservationEntity> checkOut(@PathVariable Long id,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.checkOut(id, tokenService.getCurrentUserId(servletRequest)));
    }

}

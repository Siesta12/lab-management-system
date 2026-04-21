package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.reservation.ReservationApproveRequest;
import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.dto.reservation.ReservationRecommendationRequest;
import com.nlt.domain.dto.reservation.ReservationRejectRequest;
import com.nlt.domain.vo.reservation.ReservationApplyResponse;
import com.nlt.domain.vo.reservation.ReservationConflictSlotVo;
import com.nlt.domain.vo.reservation.ReservationDetailVo;
import com.nlt.domain.vo.reservation.SlotStatusResponse;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
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

    @GetMapping
    public ApiResponse<PageData<ReservationDetailVo>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String reservationNo,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) Long applicantUserId,
        @RequestParam(required = false) Long approverUserId,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String reservationDate,
        @RequestParam(required = false) Boolean conflictOnly) {
        return ApiResponse.success(reservationService.page(pageNum, pageSize, reservationNo, labId, applicantUserId,
            approverUserId, status, reservationDate, conflictOnly));
    }

    @PostMapping
    public ApiResponse<ReservationDetailVo> create(@Valid @RequestBody ReservationCreateRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.created(reservationService.create(request, tokenService.getCurrentUserId(servletRequest)));
    }

    @PostMapping("/apply")
    public ApiResponse<ReservationApplyResponse> apply(@Valid @RequestBody ReservationCreateRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.apply(request, tokenService.getCurrentUserId(servletRequest)));
    }

    @GetMapping("/slot-status")
    public ApiResponse<SlotStatusResponse> slotStatus(@RequestParam Long labId,
        @RequestParam String date,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.slotStatus(labId, date, tokenService.getCurrentUserId(servletRequest)));
    }

    @PostMapping("/recommendations")
    public ApiResponse<List<SlotRecommendationItem>> recommend(@Valid @RequestBody ReservationRecommendationRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.recommend(request, tokenService.getCurrentUserId(servletRequest)));
    }

    @GetMapping("/my")
    public ApiResponse<PageData<ReservationDetailVo>> mine(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.mine(tokenService.getCurrentUserId(servletRequest), pageNum, pageSize));
    }

    @GetMapping("/pending-audit")
    public ApiResponse<PageData<ReservationDetailVo>> pendingAudit(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(reservationService.pendingAudit(pageNum, pageSize));
    }

    @GetMapping("/conflicts")
    public ApiResponse<PageData<ReservationConflictSlotVo>> conflicts(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(reservationService.conflictPage(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationDetailVo> getById(@PathVariable Long id) {
        return ApiResponse.success(reservationService.getById(id));
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<ReservationDetailVo> approve(@PathVariable Long id,
        @RequestBody(required = false) ReservationApproveRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.approve(id, request, tokenService.getCurrentUserId(servletRequest)));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<ReservationDetailVo> reject(@PathVariable Long id,
        @Valid @RequestBody ReservationRejectRequest request,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.reject(id, request, tokenService.getCurrentUserId(servletRequest)));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<ReservationDetailVo> cancel(@PathVariable Long id,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.cancel(id, tokenService.getCurrentUserId(servletRequest)));
    }

    @PutMapping("/{id}/check-in")
    public ApiResponse<ReservationDetailVo> checkIn(@PathVariable Long id,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.checkIn(id, tokenService.getCurrentUserId(servletRequest)));
    }

    @PutMapping("/{id}/check-out")
    public ApiResponse<ReservationDetailVo> checkOut(@PathVariable Long id,
        HttpServletRequest servletRequest) {
        return ApiResponse.success(reservationService.checkOut(id, tokenService.getCurrentUserId(servletRequest)));
    }
}


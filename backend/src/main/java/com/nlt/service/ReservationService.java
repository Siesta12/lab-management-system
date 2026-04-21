package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.reservation.ReservationApproveRequest;
import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.dto.reservation.ReservationRecommendationRequest;
import com.nlt.domain.dto.reservation.ReservationRejectRequest;
import com.nlt.domain.vo.reservation.ReservationApplyResponse;
import com.nlt.domain.vo.reservation.ReservationConflictSlotVo;
import com.nlt.domain.vo.reservation.ReservationDetailVo;
import com.nlt.domain.vo.reservation.SlotStatusResponse;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import java.util.List;

public interface ReservationService {

    PageData<ReservationDetailVo> page(int pageNum, int pageSize, String reservationNo, Long labId,
        Long applicantUserId, Long approverUserId, Integer status, String reservationDate, Boolean conflictOnly);

    PageData<ReservationDetailVo> mine(Long userId, int pageNum, int pageSize);

    PageData<ReservationDetailVo> pendingAudit(int pageNum, int pageSize);

    PageData<ReservationConflictSlotVo> conflictPage(int pageNum, int pageSize);

    ReservationDetailVo getById(Long id);

    ReservationApplyResponse apply(ReservationCreateRequest request, Long currentUserId);

    ReservationDetailVo create(ReservationCreateRequest request, Long currentUserId);

    SlotStatusResponse slotStatus(Long labId, String date, Long currentUserId);

    ReservationDetailVo approve(Long id, ReservationApproveRequest request, Long currentUserId);

    ReservationDetailVo reject(Long id, ReservationRejectRequest request, Long currentUserId);

    ReservationDetailVo cancel(Long id, Long currentUserId);

    ReservationDetailVo checkIn(Long id, Long currentUserId);

    ReservationDetailVo checkOut(Long id, Long currentUserId);

    List<SlotRecommendationItem> recommend(ReservationRecommendationRequest request, Long currentUserId);
}

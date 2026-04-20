package com.nlt.service.impl;

import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.dto.reservation.ReservationRecommendationRequest;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import com.nlt.domain.vo.schedule.ReservedSlotRow;
import com.nlt.mapper.LabReservationSlotMapper;
import com.nlt.service.ReservationConflictService;
import com.nlt.service.ReservationRecommendationService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationConflictServiceImpl implements ReservationConflictService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LabReservationSlotMapper labReservationSlotMapper;
    private final ReservationRecommendationService reservationRecommendationService;

    @Override
    public ReservationConflictResult analyze(Long currentUserId, ReservationCreateRequest request) {
        List<ReservationCreateRequest.ReservationSlotItem> slots = request.getSlots();
        LocalDate minDate = slots.stream()
            .map(item -> LocalDate.parse(item.getReservationDate(), DATE_FORMATTER))
            .min(LocalDate::compareTo)
            .orElseThrow();
        LocalDate maxDate = slots.stream()
            .map(item -> LocalDate.parse(item.getReservationDate(), DATE_FORMATTER))
            .max(LocalDate::compareTo)
            .orElseThrow();

        List<ReservedSlotRow> existingRows = labReservationSlotMapper.selectReservedSlots(request.getLabId(), minDate, maxDate);
        boolean selfPendingConflict = false;
        boolean approvedConflict = false;
        boolean pendingOthersConflict = false;
        boolean higherPriorityThanOthers = false;
        LinkedHashSet<String> notes = new LinkedHashSet<>();

        int currentPriority = calculatePriority(request.getReservationType());
        for (ReservationCreateRequest.ReservationSlotItem slot : slots) {
            LocalDate targetDate = LocalDate.parse(slot.getReservationDate(), DATE_FORMATTER);
            for (ReservedSlotRow row : existingRows) {
                if (!targetDate.equals(row.getReservationDate()) || !slot.getPeriodId().equals(row.getPeriodId())) {
                    continue;
                }
                if (row.getReservationStatus() != null && (row.getReservationStatus() == 2 || row.getReservationStatus() == 5)) {
                    approvedConflict = true;
                    notes.add("当前时段已有已通过预约，无法直接提交该时段。");
                    continue;
                }
                if (row.getReservationStatus() != null && row.getReservationStatus() == 1) {
                    if (currentUserId != null && currentUserId.equals(row.getApplicantUserId())) {
                        selfPendingConflict = true;
                        notes.add("你已申请过该时段，当前状态为待审核。");
                    } else {
                        pendingOthersConflict = true;
                        int otherPriority = calculatePriority(row.getReservationType());
                        if (currentPriority < otherPriority) {
                            higherPriorityThanOthers = true;
                            notes.add("当前时段已有低优先级申请，你的申请已进入优先审核队列。");
                        } else {
                            notes.add("当前时段已有其他用户正在申请，系统将按优先级与审核规则处理，并为你推荐其他可选方案。");
                        }
                    }
                }
            }
        }

        List<SlotRecommendationItem> recommendations = List.of();
        if (approvedConflict || pendingOthersConflict) {
            ReservationRecommendationRequest recommendationRequest = new ReservationRecommendationRequest();
            recommendationRequest.setLabId(request.getLabId());
            recommendationRequest.setParticipantCount(request.getParticipantCount());
            recommendationRequest.setSlots(request.getSlots());
            recommendations = reservationRecommendationService.recommend(recommendationRequest, currentUserId);
        }

        String note = notes.isEmpty()
            ? "当前时段可提交预约。"
            : String.join("", new ArrayList<>(notes));
        return new ReservationConflictResult(
            selfPendingConflict,
            approvedConflict,
            pendingOthersConflict,
            higherPriorityThanOthers,
            note,
            recommendations
        );
    }

    private int calculatePriority(Integer reservationType) {
        if (reservationType != null && reservationType == 1) {
            return 1;
        }
        if (reservationType != null && reservationType == 2) {
            return 2;
        }
        return 3;
    }
}

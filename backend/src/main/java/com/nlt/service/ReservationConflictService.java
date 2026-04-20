package com.nlt.service;

import com.nlt.domain.dto.reservation.ReservationCreateRequest;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import java.util.List;

public interface ReservationConflictService {

    ReservationConflictResult analyze(Long currentUserId, ReservationCreateRequest request);

    record ReservationConflictResult(
        boolean selfPendingConflict,
        boolean approvedConflict,
        boolean pendingOthersConflict,
        boolean higherPriorityThanOthers,
        String conflictNote,
        List<SlotRecommendationItem> recommendations
    ) { }
}

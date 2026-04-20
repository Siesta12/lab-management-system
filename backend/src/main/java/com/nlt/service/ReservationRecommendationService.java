package com.nlt.service;

import com.nlt.domain.dto.reservation.ReservationRecommendationRequest;
import com.nlt.domain.vo.reservation.SlotRecommendationItem;
import java.util.List;

public interface ReservationRecommendationService {

    List<SlotRecommendationItem> recommend(ReservationRecommendationRequest request, Long currentUserId);
}

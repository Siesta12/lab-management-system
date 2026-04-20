package com.nlt.domain.vo.reservation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReservationApplyResponse {

    private boolean submitted;

    private String currentStatus;

    private boolean conflict;

    private String conflictNote;

    private ReservationDetailVo reservation;

    private List<SlotRecommendationItem> recommendations;
}

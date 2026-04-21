package com.nlt.domain.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayReservationSummaryVo {

    private long total;

    private long morning;

    private long afternoon;

    private long evening;

    private long pending;

    private long conflict;

    private long upcoming;
}

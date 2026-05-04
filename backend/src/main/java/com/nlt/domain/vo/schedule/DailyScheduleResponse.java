package com.nlt.domain.vo.schedule;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailyScheduleResponse {

    private String date;

    private Integer weekday;

    private List<SchedulePeriodItem> periods;

    private List<DailyScheduleLabItem> labs;
}


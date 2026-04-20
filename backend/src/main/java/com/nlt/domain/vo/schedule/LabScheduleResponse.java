package com.nlt.domain.vo.schedule;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabScheduleResponse {

    private String startDate;

    private String endDate;

    private List<SchedulePeriodItem> periods;

    private List<ScheduleDayItem> days;
}



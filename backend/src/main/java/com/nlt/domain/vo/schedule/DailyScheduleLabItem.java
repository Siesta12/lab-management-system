package com.nlt.domain.vo.schedule;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailyScheduleLabItem {

    private Long labId;

    private String labName;

    private List<ScheduleCellItem> cells;
}



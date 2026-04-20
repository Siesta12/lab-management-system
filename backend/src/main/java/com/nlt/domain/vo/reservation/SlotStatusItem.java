package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SlotStatusItem {

    private Long periodId;

    private String periodName;

    private String status;

    private String displayText;

    private String note;
}

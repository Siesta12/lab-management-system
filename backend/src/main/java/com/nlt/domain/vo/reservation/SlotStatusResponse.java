package com.nlt.domain.vo.reservation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SlotStatusResponse {

    private Long labId;

    private String date;

    private List<SlotStatusItem> slots;
}

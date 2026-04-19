package com.nlt.domain.vo.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConflictCheckData {

    private boolean conflicted;

    private int conflictCount;

    private String message;

}

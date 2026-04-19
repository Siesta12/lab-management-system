package com.nlt.domain.dto.violation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ViolationSaveRequest {

    @NotNull
    private Long userId;

    private Long reservationId;

    @NotNull
    private Integer violationType;

    @NotNull
    private Integer scoreChange;

    private String remark;

}

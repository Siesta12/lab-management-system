package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConflictCheckRequest {

    @NotNull
    private Long labId;

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

}

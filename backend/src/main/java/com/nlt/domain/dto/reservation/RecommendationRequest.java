package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendationRequest {

    @NotNull
    private Long labId;

    @NotBlank
    private String reservationDate;

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

    private Integer participantCount;

}

package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationCreateRequest {

    @NotNull
    private Long labId;

    private Integer reservationType;

    private Integer priorityLevel;

    @NotBlank
    private String reservationDate;

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

    @NotBlank
    private String usagePurpose;

    private String courseOrProjectName;

    private Integer participantCount;

    private String contactPhone;

}

package com.nlt.domain.dto.lab;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LabOpenRuleSaveRequest {

    @NotNull
    private Long labId;

    @NotNull
    private Integer weekday;

    @NotNull
    private String startTime;

    @NotNull
    private String endTime;

    private Integer allowStudent;

    private Integer allowTeacher;

    private Integer maxReservationHours;

    private Integer status;

}

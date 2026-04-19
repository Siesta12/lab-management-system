package com.nlt.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
public class LabOpenRuleEntity {

    private Long id;

    private Long labId;

    private Integer weekday;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer allowStudent;

    private Integer allowTeacher;

    private Integer maxReservationHours;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}

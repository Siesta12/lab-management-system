package com.nlt.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
public class ClassPeriodEntity {

    private Long id;

    private Integer periodNo;

    private String periodName;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}



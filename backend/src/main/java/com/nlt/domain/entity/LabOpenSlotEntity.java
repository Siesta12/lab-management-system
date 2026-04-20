package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LabOpenSlotEntity {

    private Long id;

    private Long labId;

    private Integer weekday;

    private Long periodId;

    private Integer allowStudent;

    private Integer allowTeacher;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}



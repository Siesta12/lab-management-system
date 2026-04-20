package com.nlt.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LabMaintenanceEntity {

    private Long id;

    private Long labId;

    private LocalDate maintenanceDate;

    private Long periodId;

    private String reason;

    private Integer status;

    private Long operatorUserId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}



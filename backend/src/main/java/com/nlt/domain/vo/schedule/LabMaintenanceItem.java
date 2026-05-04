package com.nlt.domain.vo.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabMaintenanceItem {

    private Long id;

    private Long labId;

    private String maintenanceDate;

    private Integer weekday;

    private Long periodId;

    private String periodName;

    private String reason;

    private Integer status;

    private Long operatorUserId;
}


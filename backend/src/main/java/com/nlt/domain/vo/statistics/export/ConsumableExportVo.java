package com.nlt.domain.vo.statistics.export;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ConsumableExportVo {

    private String consumableName;

    private String labName;

    private String labType;

    private Integer stockQuantity;

    private Integer warningThreshold;

    private String unit;

    private Integer inQuantity;

    private Integer outQuantity;

    private LocalDateTime lastInTime;

    private LocalDateTime lastOutTime;

    private Integer lowStock;
}

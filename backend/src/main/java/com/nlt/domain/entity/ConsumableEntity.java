package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ConsumableEntity {

    private Long id;

    private Long labId;

    private String consumableName;

    private String consumableCode;

    private String specification;

    private String unit;

    private Integer stockQuantity;

    private Integer warningThreshold;

    private Integer status;

    private String remark;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}


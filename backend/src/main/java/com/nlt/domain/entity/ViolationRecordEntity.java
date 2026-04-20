package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ViolationRecordEntity {

    private Long id;

    private Long userId;

    private Long reservationId;

    private Integer violationType;

    private Integer scoreChange;

    private String remark;

    private LocalDateTime createdAt;

}


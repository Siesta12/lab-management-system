package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ConsumableStockLogEntity {

    private Long id;

    private Long consumableId;

    private String changeType;

    private Integer changeAmount;

    private Integer beforeStock;

    private Integer afterStock;

    private Long operatorUserId;

    private String remark;

    private LocalDateTime createdAt;

}

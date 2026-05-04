package com.nlt.domain.dto.consumable;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsumableStockUpdateRequest {

    private String changeType;

    private Integer quantity;

    private Integer targetStock;

    private String remark;

}


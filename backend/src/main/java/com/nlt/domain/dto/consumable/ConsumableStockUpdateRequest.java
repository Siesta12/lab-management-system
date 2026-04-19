package com.nlt.domain.dto.consumable;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsumableStockUpdateRequest {

    @NotNull
    private Integer stockQuantity;

    private String changeType;

    private String remark;

}

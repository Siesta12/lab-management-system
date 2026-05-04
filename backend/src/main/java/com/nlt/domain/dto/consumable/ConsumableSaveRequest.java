package com.nlt.domain.dto.consumable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsumableSaveRequest {

    @NotNull
    private Long labId;

    @NotBlank
    private String consumableName;

    @NotBlank
    private String unit;

    private Integer stockQuantity;

    private Integer warningThreshold;

    private Integer status;

    private String remark;

}


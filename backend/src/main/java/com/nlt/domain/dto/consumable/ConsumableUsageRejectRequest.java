package com.nlt.domain.dto.consumable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsumableUsageRejectRequest {

    @NotBlank
    private String rejectReason;
}

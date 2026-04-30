package com.nlt.domain.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceRepairCreateRequest {

    @NotNull
    private Long deviceId;

    @NotBlank
    private String issueDescription;

    private Integer urgencyLevel;
}

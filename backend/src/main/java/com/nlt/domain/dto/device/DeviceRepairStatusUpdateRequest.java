package com.nlt.domain.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceRepairStatusUpdateRequest {

    @NotNull
    private Integer status;

    private String handlingResult;

    private Integer deviceStatus;
}

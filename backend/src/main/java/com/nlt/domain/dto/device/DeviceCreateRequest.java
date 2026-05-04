package com.nlt.domain.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceCreateRequest {

    @NotNull
    private Long labId;

    @NotBlank
    private String deviceName;

    private String brand;

    private String modelNo;

    private Integer quantity;

    private Integer availableQuantity;

    private Integer status;

    private String purchaseDate;

    private String remark;
}

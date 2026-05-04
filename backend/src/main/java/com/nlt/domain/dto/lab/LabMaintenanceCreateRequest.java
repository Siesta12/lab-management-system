package com.nlt.domain.dto.lab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class LabMaintenanceCreateRequest {

    @NotBlank
    private String maintenanceDate;

    @NotEmpty
    private List<@NotNull Long> periodIds;

    @NotBlank
    private String reason;
}


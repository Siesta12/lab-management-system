package com.nlt.domain.dto.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleSaveRequest {

    @NotBlank
    private String roleName;

    @NotBlank
    private String roleCode;

    private String description;

    private Integer status;

}


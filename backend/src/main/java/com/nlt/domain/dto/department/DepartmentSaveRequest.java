package com.nlt.domain.dto.department;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentSaveRequest {

    @NotBlank
    private String departmentName;

    @NotBlank
    private String departmentCode;

    private String leaderName;

    private String phone;

    private Integer status;

}


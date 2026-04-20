package com.nlt.domain.dto.lab;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LabSaveRequest {

    private Long departmentId;

    @NotBlank
    private String labCode;

    @NotBlank
    private String labName;

    private String labType;

    private String buildingName;

    private String roomNo;

    private Integer capacity;

    private Long managerUserId;

    private Integer openStatus;

    private Integer labStatus;

    private String description;

    private String usageRule;

}


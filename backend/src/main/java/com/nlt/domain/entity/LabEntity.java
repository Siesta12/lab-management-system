package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LabEntity {

    private Long id;

    private Long departmentId;

    private String labCode;

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

    private Double latitude;

    private Double longitude;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}


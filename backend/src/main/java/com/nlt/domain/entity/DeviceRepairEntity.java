package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DeviceRepairEntity {

    private Long id;

    private Long deviceId;

    private Long labId;

    private Long applicantUserId;

    private String deviceName;

    private String deviceCode;

    private String labName;

    private String applicantName;

    private String issueDescription;

    private Integer urgencyLevel;

    private Integer status;

    private Long handlerUserId;

    private String handlerName;

    private String handlingResult;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

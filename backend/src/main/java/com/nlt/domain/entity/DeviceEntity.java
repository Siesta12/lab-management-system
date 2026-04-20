package com.nlt.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DeviceEntity {

    private Long id;

    private Long labId;

    private String deviceName;

    private String deviceCode;

    private String brand;

    private String modelNo;

    private Integer quantity;

    private Integer availableQuantity;

    private Integer status;

    private LocalDate purchaseDate;

    private String remark;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}


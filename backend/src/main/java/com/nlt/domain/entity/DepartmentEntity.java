package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DepartmentEntity {

    private Long id;

    private String departmentName;

    private String departmentCode;

    private String leaderName;

    private String phone;

    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}


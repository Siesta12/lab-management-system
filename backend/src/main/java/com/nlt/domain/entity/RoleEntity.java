package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RoleEntity {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}

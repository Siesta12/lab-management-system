package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserRoleEntity {

    private Long id;

    private Long userId;

    private Long roleId;

    private LocalDateTime createdAt;

}


package com.nlt.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserEntity {

    private Long id;

    private Long departmentId;

    private String username;

    private String password;

    private String realName;

    private String userNo;

    private Integer gender;

    private String phone;

    private String email;

    private Integer creditScore;

    private Integer violationCount;

    private Integer status;

    private Integer deleted;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}


package com.nlt.domain.dto.user;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    private String realName;

    private Integer gender;

    private String phone;

    private String email;

}


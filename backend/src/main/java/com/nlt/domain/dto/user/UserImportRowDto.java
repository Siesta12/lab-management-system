package com.nlt.domain.dto.user;

import lombok.Data;

@Data
public class UserImportRowDto {

    private int rowNum;

    private String userNo;

    private String realName;

    private String gender;

    private String phone;

    private String email;

    private String departmentName;

    private String roleName;
}

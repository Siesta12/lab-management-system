package com.nlt.domain.dto.user;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {

    private Long departmentId;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String realName;

    private String userNo;

    private Integer gender;

    private String phone;

    private String email;

    private Integer status;

    private List<Long> roleIds;

}

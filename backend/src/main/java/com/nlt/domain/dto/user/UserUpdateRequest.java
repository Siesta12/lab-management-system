package com.nlt.domain.dto.user;

import java.util.List;
import lombok.Data;

@Data
public class UserUpdateRequest {

    private Long departmentId;

    private String realName;

    private String userNo;

    private Integer gender;

    private String phone;

    private String email;

    private Integer status;

    private List<Long> roleIds;

}

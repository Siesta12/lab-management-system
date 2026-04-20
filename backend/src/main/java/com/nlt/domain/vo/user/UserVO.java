package com.nlt.domain.vo.user;

import java.util.List;
import lombok.Data;

@Data
public class UserVO {

    private Long id;

    private Long departmentId;

    private String username;

    private String realName;

    private String userNo;

    private Integer gender;

    private String phone;

    private String email;

    private Integer creditScore;

    private Integer violationCount;

    private Integer status;

    private List<Long> roleIds;

}


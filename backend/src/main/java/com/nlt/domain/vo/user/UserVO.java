package com.nlt.domain.vo.user;

import java.util.List;
import lombok.Data;

@Data
public class UserVO {

    private Long id;

    private Long departmentId;

    private String userNo;

    private String realName;

    private Integer gender;

    private String phone;

    private String email;

    private Integer creditScore;

    private Integer violationCount;

    private Integer status;

    private List<Long> roleIds;

}


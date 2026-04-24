package com.nlt.domain.vo.auth;

import java.util.List;
import lombok.Data;

@Data
public class LoginResponseData {

    private Long id;

    private String userNo;

    private String realName;

    private String token;

    private List<String> roleCodes;

    private Long departmentId;

}


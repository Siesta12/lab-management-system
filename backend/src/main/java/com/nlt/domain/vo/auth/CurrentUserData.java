package com.nlt.domain.vo.auth;

import java.util.List;
import lombok.Data;

@Data
public class CurrentUserData {

    private Long id;

    private String userNo;

    private String realName;

    private List<String> roleCodes;

    private Long departmentId;

}


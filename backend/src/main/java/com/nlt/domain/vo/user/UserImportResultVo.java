package com.nlt.domain.vo.user;

import java.util.List;
import lombok.Data;

@Data
public class UserImportResultVo {

    private int total;

    private int success;

    private int fail;

    private List<UserImportFailDetailVo> failDetails;
}

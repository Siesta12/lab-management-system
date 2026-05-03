package com.nlt.domain.vo.statistics.export;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CreditViolationExportVo {

    private String realName;

    private String userNo;

    private String userRole;

    private Integer creditScore;

    private Integer violationType;

    private Integer scoreChange;

    private LocalDateTime violationTime;

    private String reservationNo;

    private Long totalViolationCount;

    private String remark;
}

package com.nlt.domain.vo.checkin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到模块统一返回对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResultVo {

    private String action;

    private Long reservationId;

    private String reservationNo;

    private Long labId;

    private String labName;

    private String reservationDate;

    private String periodName;

    private String checkInTime;

    private boolean late;

    private int scoreChange;

    private long distanceMeters;

    private String message;
}

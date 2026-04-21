package com.nlt.domain.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAlertVo {

    private String title;

    private String detail;

    private String tag;
}

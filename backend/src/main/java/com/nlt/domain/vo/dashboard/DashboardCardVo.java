package com.nlt.domain.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCardVo {

    private String label;

    private String value;

    private String trend;

    private String tone;
}

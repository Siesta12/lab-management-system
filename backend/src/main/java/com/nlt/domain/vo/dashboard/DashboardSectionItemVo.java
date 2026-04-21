package com.nlt.domain.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSectionItemVo {

    private String primary;

    private String secondary;

    private String meta;

    private String statusLabel;

    private String tone;
}

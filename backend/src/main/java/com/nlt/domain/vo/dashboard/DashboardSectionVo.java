package com.nlt.domain.vo.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSectionVo {

    private String title;

    private String tag;

    private List<DashboardSectionItemVo> items;
}

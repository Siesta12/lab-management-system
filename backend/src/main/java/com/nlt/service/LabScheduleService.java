package com.nlt.service;

import com.nlt.domain.dto.lab.LabMaintenanceCreateRequest;
import com.nlt.domain.vo.schedule.DailyScheduleResponse;
import com.nlt.domain.vo.schedule.LabMaintenanceItem;
import com.nlt.domain.vo.schedule.LabScheduleResponse;
import java.util.List;

public interface LabScheduleService {

    LabScheduleResponse getLabSchedule(Long labId, String startDate, Long currentUserId, List<String> currentRoleCodes);

    DailyScheduleResponse getDailySchedule(String date, Long currentUserId, List<String> currentRoleCodes);

    List<LabMaintenanceItem> listLabMaintenance(Long labId, Long currentUserId, List<String> currentRoleCodes);

    List<LabMaintenanceItem> createMaintenance(Long labId, LabMaintenanceCreateRequest request, Long operatorUserId,
        List<String> currentRoleCodes);

    void cancelMaintenance(Long labId, Long maintenanceId, Long operatorUserId, List<String> currentRoleCodes);
}



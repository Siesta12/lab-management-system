package com.nlt.mapper;

import com.nlt.domain.entity.LabMaintenanceEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabMaintenanceMapper {

    int upsertBatch(@Param("list") List<LabMaintenanceEntity> list);

    LabMaintenanceEntity selectById(@Param("id") Long id);

    List<LabMaintenanceEntity> selectByLabAndDateRange(@Param("labId") Long labId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    List<LabMaintenanceEntity> selectActiveByLabAndDateAndPeriods(@Param("labId") Long labId,
        @Param("maintenanceDate") LocalDate maintenanceDate,
        @Param("periodIds") List<Long> periodIds);

    List<LabMaintenanceEntity> selectActiveByDate(@Param("maintenanceDate") LocalDate maintenanceDate);

    int cancel(@Param("id") Long id, @Param("operatorUserId") Long operatorUserId);
}

package com.nlt.mapper;

import com.nlt.domain.entity.LabOpenSlotEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabOpenSlotMapper {

    List<LabOpenSlotEntity> selectList(@Param("labId") Long labId, @Param("weekday") Integer weekday);

    List<LabOpenSlotEntity> selectActiveByLabAndWeekdaysAndPeriods(@Param("labId") Long labId,
        @Param("weekdays") List<Integer> weekdays,
        @Param("periodIds") List<Long> periodIds);

    List<LabOpenSlotEntity> selectActiveByLabIdsAndWeekdayAndPeriods(@Param("labIds") List<Long> labIds,
        @Param("weekday") Integer weekday,
        @Param("periodIds") List<Long> periodIds);
}

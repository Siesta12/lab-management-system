package com.nlt.mapper;

import com.nlt.domain.entity.LabEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabMapper {

    List<LabEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("labId") Long labId, @Param("labName") String labName, @Param("labCode") String labCode,
    @Param("labType") String labType, @Param("departmentId") Long departmentId,
    @Param("openStatus") Integer openStatus, @Param("labStatus") Integer labStatus);

    long countPage(@Param("labId") Long labId, @Param("labName") String labName, @Param("labCode") String labCode,
    @Param("labType") String labType, @Param("departmentId") Long departmentId,
    @Param("openStatus") Integer openStatus, @Param("labStatus") Integer labStatus);

    LabEntity selectById(@Param("id") Long id);

    List<LabEntity> selectOptions(@Param("openStatus") Integer openStatus, @Param("departmentId") Long departmentId);

    int insert(LabEntity entity);

    int update(LabEntity entity);

    int updateOpenStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateLabStatus(@Param("id") Long id, @Param("status") Integer status);

    int softDelete(@Param("id") Long id);

    List<LabEntity> selectRecommendationCandidates(@Param("excludeLabId") Long excludeLabId,
    @Param("participantCount") Integer participantCount);

}


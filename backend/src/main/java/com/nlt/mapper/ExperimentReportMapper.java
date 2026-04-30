package com.nlt.mapper;

import com.nlt.domain.entity.ExperimentReportConsumableEntity;
import com.nlt.domain.entity.ExperimentReportEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExperimentReportMapper {

    List<ExperimentReportEntity> selectPage(@Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("departmentId") Long departmentId,
        @Param("studentId") Long studentId,
        @Param("teacherId") Long teacherId,
        @Param("status") Integer status,
        @Param("keyword") String keyword);

    long countPage(@Param("departmentId") Long departmentId,
        @Param("studentId") Long studentId,
        @Param("teacherId") Long teacherId,
        @Param("status") Integer status,
        @Param("keyword") String keyword);

    ExperimentReportEntity selectById(@Param("id") Long id);

    List<ExperimentReportConsumableEntity> selectConsumables(@Param("reportId") Long reportId);

    int insert(ExperimentReportEntity entity);

    int updateDraft(ExperimentReportEntity entity);

    int submit(@Param("id") Long id);

    int review(@Param("id") Long id, @Param("status") Integer status,
        @Param("teacherComment") String teacherComment,
        @Param("teacherId") Long teacherId);

    int deleteConsumables(@Param("reportId") Long reportId);

    int insertConsumable(ExperimentReportConsumableEntity entity);
}

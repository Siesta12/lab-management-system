package com.nlt.mapper;

import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.vo.statistics.export.ConsumableExportVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumableMapper {

    List<ConsumableEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                      @Param("labId") Long labId, @Param("labType") String labType, @Param("consumableName") String consumableName,
                                      @Param("consumableCode") String consumableCode,
                                      @Param("departmentId") Long departmentId,
                                      @Param("status") Integer status,
                                      @Param("warningOnly") boolean warningOnly);

    long countPage(@Param("labId") Long labId, @Param("labType") String labType, @Param("consumableName") String consumableName,
                   @Param("consumableCode") String consumableCode,
                   @Param("departmentId") Long departmentId,
                   @Param("status") Integer status,
                   @Param("warningOnly") boolean warningOnly);

    ConsumableEntity selectById(@Param("id") Long id);

    ConsumableEntity selectByIdForUpdate(@Param("id") Long id);

    String selectLatestConsumableCodeForUpdate();

    List<ConsumableEntity> selectAvailableOptions(@Param("labId") Long labId);

    List<ConsumableEntity> selectWarningList(@Param("departmentId") Long departmentId);

    int insert(ConsumableEntity entity);

    int update(ConsumableEntity entity);

    int updateStock(@Param("id") Long id, @Param("stockQuantity") Integer stockQuantity);

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    int softDelete(@Param("id") Long id);

    long countExport(@Param("departmentId") Long departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labType") String labType,
        @Param("labId") Long labId);

    List<ConsumableExportVo> selectExportList(@Param("departmentId") Long departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labType") String labType,
        @Param("labId") Long labId,
        @Param("limit") int limit);

}


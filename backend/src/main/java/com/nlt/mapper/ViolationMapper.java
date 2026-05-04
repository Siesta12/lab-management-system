package com.nlt.mapper;

import com.nlt.domain.entity.ViolationRecordEntity;
import com.nlt.domain.vo.statistics.export.CreditViolationExportVo;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ViolationMapper {

    List<ViolationRecordEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("userId") Long userId, @Param("reservationId") Long reservationId,
    @Param("violationType") Integer violationType);

    long countPage(@Param("userId") Long userId, @Param("reservationId") Long reservationId,
    @Param("violationType") Integer violationType);

    ViolationRecordEntity selectById(@Param("id") Long id);

    List<ViolationRecordEntity> selectMine(@Param("userId") Long userId,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("violationType") Integer violationType,
        @Param("scoreDirection") Integer scoreDirection);

    long countMine(@Param("userId") Long userId,
        @Param("violationType") Integer violationType,
        @Param("scoreDirection") Integer scoreDirection);

    int insert(ViolationRecordEntity entity);

    boolean existsByReservationAndType(@Param("reservationId") Long reservationId,
        @Param("violationType") Integer violationType);

    boolean existsByReservationId(@Param("reservationId") Long reservationId);

    int deleteById(@Param("id") Long id);

    List<Map<String, Object>> countByType(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("departmentId") Long departmentId,
    @Param("violationType") Integer violationType);

    long countExport(@Param("departmentId") Long departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labType") String labType,
        @Param("labId") Long labId,
        @Param("status") Integer status,
        @Param("reservationType") Integer reservationType);

    List<CreditViolationExportVo> selectExportList(@Param("departmentId") Long departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labType") String labType,
        @Param("labId") Long labId,
        @Param("status") Integer status,
        @Param("reservationType") Integer reservationType,
        @Param("limit") int limit);

}


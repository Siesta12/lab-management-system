package com.nlt.mapper;

import com.nlt.domain.entity.ReservationEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationMapper {

    List<ReservationEntity> selectPage(@Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("reservationNo") String reservationNo,
        @Param("labId") Long labId,
        @Param("applicantUserId") Long applicantUserId,
        @Param("approverUserId") Long approverUserId,
        @Param("status") Integer status,
        @Param("reservationDate") LocalDate reservationDate);

    long countPage(@Param("reservationNo") String reservationNo,
        @Param("labId") Long labId,
        @Param("applicantUserId") Long applicantUserId,
        @Param("approverUserId") Long approverUserId,
        @Param("status") Integer status,
        @Param("reservationDate") LocalDate reservationDate);

    ReservationEntity selectById(@Param("id") Long id);

    List<ReservationEntity> selectMine(@Param("userId") Long userId, @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countMine(@Param("userId") Long userId);

    List<ReservationEntity> selectPendingAudit(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPendingAudit();

    int insert(ReservationEntity entity);

    int updateAuditResult(ReservationEntity entity);

    int cancel(@Param("id") Long id);

    int checkIn(@Param("id") Long id);

    int checkOut(@Param("id") Long id);

    List<Map<String, Object>> countByReservationType(@Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labId") Long labId);

    List<Map<String, Object>> countByStatus(@Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labId") Long labId);

    List<Map<String, Object>> countByLabUsage(@Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("departmentId") Long departmentId,
        @Param("labId") Long labId);

    List<Map<String, Object>> countByTimeDistribution(@Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labId") Long labId);

    List<Map<String, Object>> reservationTrend(@Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}


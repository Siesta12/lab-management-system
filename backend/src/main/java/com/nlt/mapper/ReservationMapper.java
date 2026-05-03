package com.nlt.mapper;

import com.nlt.domain.entity.ReservationEntity;
import com.nlt.domain.vo.statistics.export.ReservationExportVo;
import com.nlt.domain.vo.reservation.ReservationCheckCandidateVo;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        @Param("reservationDate") LocalDate reservationDate,
        @Param("departmentId") Long departmentId,
        @Param("conflictOnly") boolean conflictOnly);

    long countPage(@Param("reservationNo") String reservationNo,
        @Param("labId") Long labId,
        @Param("applicantUserId") Long applicantUserId,
        @Param("approverUserId") Long approverUserId,
        @Param("status") Integer status,
        @Param("reservationDate") LocalDate reservationDate,
        @Param("departmentId") Long departmentId,
        @Param("conflictOnly") boolean conflictOnly);

    ReservationEntity selectById(@Param("id") Long id);

    List<ReservationEntity> selectMine(@Param("userId") Long userId,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("status") Integer status,
        @Param("reservationType") Integer reservationType);

    long countMine(@Param("userId") Long userId,
        @Param("status") Integer status,
        @Param("reservationType") Integer reservationType);

    List<ReservationEntity> selectPendingAudit(@Param("offset") int offset, @Param("pageSize") int pageSize,
        @Param("departmentId") Long departmentId);

    long countPendingAudit(@Param("departmentId") Long departmentId);

    int insert(ReservationEntity entity);

    int updateAuditResult(ReservationEntity entity);

    int cancel(@Param("id") Long id);

    int checkIn(@Param("id") Long id);

    int checkInAt(@Param("id") Long id, @Param("checkInTime") LocalDateTime checkInTime);

    List<ReservationCheckCandidateVo> selectCheckInCandidates(@Param("userId") Long userId,
        @Param("labId") Long labId,
        @Param("reservationDate") LocalDate reservationDate);

    List<ReservationCheckCandidateVo> selectCheckInCandidatesByLab(@Param("labId") Long labId,
        @Param("reservationDate") LocalDate reservationDate);

    List<ReservationCheckCandidateVo> selectNoShowCandidates(@Param("currentDate") LocalDate currentDate);

    List<ReservationCheckCandidateVo> selectCompletedCandidates(@Param("currentDateTime") LocalDateTime currentDateTime);

    int markCompleted(@Param("id") Long id);

    List<com.nlt.domain.vo.dashboard.DashboardReservationItemVo> selectDashboardDailySlots(@Param("date") LocalDate date,
        @Param("departmentId") Long departmentId);

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

    long countExport(@Param("departmentId") Long departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labType") String labType,
        @Param("labId") Long labId,
        @Param("status") Integer status,
        @Param("reservationType") Integer reservationType);

    List<ReservationExportVo> selectExportList(@Param("departmentId") Long departmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("labType") String labType,
        @Param("labId") Long labId,
        @Param("status") Integer status,
        @Param("reservationType") Integer reservationType,
        @Param("limit") int limit);
}



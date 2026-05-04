package com.nlt.mapper;

import com.nlt.domain.entity.LabReservationSlotEntity;
import com.nlt.domain.vo.reservation.ReservationConflictRowVo;
import com.nlt.domain.vo.reservation.ReservationSlotVo;
import com.nlt.domain.vo.schedule.ReservedSlotRow;
import com.nlt.domain.vo.schedule.ReservedSlotRowWithLab;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabReservationSlotMapper {

    int insertBatch(@Param("list") List<LabReservationSlotEntity> list);

    List<ReservationSlotVo> selectDetailByReservationId(@Param("reservationId") Long reservationId);

    List<ReservationSlotVo> selectDetailByReservationIds(@Param("reservationIds") List<Long> reservationIds);

    int cancelByReservationId(@Param("reservationId") Long reservationId);

    int deleteCanceledSlots(@Param("list") List<LabReservationSlotEntity> list);

    List<Long> selectPendingConflictReservationIds(@Param("reservationId") Long reservationId);

    List<ReservedSlotRow> selectReservedSlots(@Param("labId") Long labId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    List<ReservedSlotRowWithLab> selectReservedSlotsForDate(@Param("reservationDate") LocalDate reservationDate);

    List<ReservationConflictRowVo> selectConflictRowsByDepartment(@Param("departmentId") Long departmentId);
}


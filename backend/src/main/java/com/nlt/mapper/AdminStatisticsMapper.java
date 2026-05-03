package com.nlt.mapper;

import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionItemVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminStatisticsMapper {

    List<OptionItemVo> selectLabOptions(@Param("departmentId") Long departmentId);

    long countAccessibleLab(@Param("departmentId") Long departmentId, @Param("labId") Long labId);

    long countOverviewMonthReservations(@Param("departmentId") Long departmentId, @Param("labType") String labType,
                                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long countTodayReservations(@Param("departmentId") Long departmentId, @Param("today") LocalDate today);

    long countPendingReservations(@Param("departmentId") Long departmentId);

    long countBrokenDevices(@Param("departmentId") Long departmentId);

    long countRepairingDevices(@Param("departmentId") Long departmentId);

    long countLowStockConsumables(@Param("departmentId") Long departmentId);

    long countOverviewMonthViolations(@Param("departmentId") Long departmentId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    BigDecimal selectAverageCreditScore(@Param("departmentId") Long departmentId);

    long countLowCreditUsers(@Param("departmentId") Long departmentId, @Param("lowCreditScore") Integer lowCreditScore);

    long countReservationTotal(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                               @Param("labType") String labType, @Param("status") Integer status,
                               @Param("reservationType") Integer reservationType, @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    long countReservationPending(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                 @Param("labType") String labType, @Param("reservationType") Integer reservationType,
                                 @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long countReservationCompleted(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                   @Param("labType") String labType, @Param("reservationType") Integer reservationType,
                                   @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectReservationTrend(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                     @Param("labType") String labType, @Param("status") Integer status,
                                                     @Param("reservationType") Integer reservationType,
                                                     @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectReservationStatusDistribution(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                                  @Param("labType") String labType,
                                                                  @Param("reservationType") Integer reservationType,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectReservationTypeDistribution(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                                @Param("labType") String labType, @Param("status") Integer status,
                                                                @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectReservationApplicantRoleDistribution(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                                         @Param("labType") String labType, @Param("status") Integer status,
                                                                         @Param("reservationType") Integer reservationType,
                                                                         @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectReservationLabTypeDistribution(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                                   @Param("labType") String labType,
                                                                   @Param("status") Integer status,
                                                                   @Param("reservationType") Integer reservationType,
                                                                   @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectLabUsageRanking(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                    @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectLabUsageIdleLabs(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                     @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectLabUsageTimeHeat(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                     @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    long countHighUsageLabs(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                            @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    long countIdleLabs(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                       @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);

    long countLabUsageOccupiedSlots(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                    @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectUsageLabTypes(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                  @Param("selectedLabType") String selectedLabType);

    long countLabTypeOccupied(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                              @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectOpenSlotWeekdayCounts(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                          @Param("labType") String labType);

    long countOpenSlotLabCount(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                               @Param("labType") String labType);

    long countActiveClassPeriods();

    long countDevicesTotal(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                           @Param("labType") String labType);

    long countDevicesByStatus(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                              @Param("labType") String labType, @Param("deviceStatus") Integer deviceStatus);

    long countDeviceRepairOrders(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                 @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectLabDeviceCounts(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                    @Param("labType") String labType);

    List<Map<String, Object>> selectAbnormalDevices(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                    @Param("labType") String labType);

    List<Map<String, Object>> selectDeviceCategoryDistribution(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                               @Param("labType") String labType);

    List<Map<String, Object>> selectDeviceRepairTrend(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                      @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    long countConsumableTypes(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                              @Param("labType") String labType);

    long countLowStockConsumablesByQuery(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                         @Param("labType") String labType);

    long sumConsumableInQuantity(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                 @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    long sumConsumableOutQuantity(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                  @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectConsumableConsumptionRanking(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                                 @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                                 @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectConsumableWarningList(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                          @Param("labType") String labType);

    List<Map<String, Object>> selectConsumableInTrend(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                      @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectConsumableOutTrend(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                       @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectConsumableLabUsageRanking(@Param("departmentId") Long departmentId, @Param("labId") Long labId,
                                                              @Param("labType") String labType, @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    long countViolationsByType(@Param("departmentId") Long departmentId, @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate, @Param("violationType") Integer violationType);

    List<Map<String, Object>> selectViolationTypeDistribution(@Param("departmentId") Long departmentId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectLowCreditUsers(@Param("departmentId") Long departmentId,
                                                   @Param("lowCreditScore") Integer lowCreditScore);

    List<Map<String, Object>> selectViolationRanking(@Param("departmentId") Long departmentId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectViolationTrend(@Param("departmentId") Long departmentId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectLateTrend(@Param("departmentId") Long departmentId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectNoShowTrend(@Param("departmentId") Long departmentId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectCreditScoreDistribution(@Param("departmentId") Long departmentId);

    List<Map<String, Object>> selectRoleViolationDistribution(@Param("departmentId") Long departmentId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectCreditReservationTypeDistribution(@Param("departmentId") Long departmentId,
                                                                      @Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> selectCreditTimeSegmentDistribution(@Param("departmentId") Long departmentId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);
}

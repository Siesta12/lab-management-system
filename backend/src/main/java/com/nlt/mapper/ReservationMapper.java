package com.nlt.mapper;

import com.nlt.domain.entity.ReservationEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationMapper {

    /**
     * 查询预约信息
     * @param offset 参数
     * @param pageSize 每页条数
     * @param reservationNo 预约编号
     * @param labId 实验室ID
     * @param applicantUserId 申请人用户ID
     * @param approverUserId 审批人用户ID
     * @param status 状态值
     * @param reservationDate 预约日期
     * @return 数据列表
     */
    List<ReservationEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("reservationNo") String reservationNo, @Param("labId") Long labId,
    @Param("applicantUserId") Long applicantUserId,
    @Param("approverUserId") Long approverUserId,
    @Param("status") Integer status,
    @Param("reservationDate") LocalDate reservationDate);

    /**
     * 统计预约信息数量
     * @param reservationNo 预约编号
     * @param labId 实验室ID
     * @param applicantUserId 申请人用户ID
     * @param approverUserId 审批人用户ID
     * @param status 状态值
     * @param reservationDate 预约日期
     * @return 处理结果
     */
    long countPage(@Param("reservationNo") String reservationNo, @Param("labId") Long labId,
    @Param("applicantUserId") Long applicantUserId, @Param("approverUserId") Long approverUserId,
    @Param("status") Integer status, @Param("reservationDate") LocalDate reservationDate);

    /**
     * 查询预约信息
     * @param id 主键ID
     * @return 处理结果
     */
    ReservationEntity selectById(@Param("id") Long id);

    /**
     * 查询预约信息
     * @param userId 用户ID
     * @param offset 参数
     * @param pageSize 每页条数
     * @return 数据列表
     */
    List<ReservationEntity> selectMine(@Param("userId") Long userId, @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计预约信息数量
     * @param userId 用户ID
     * @return 处理结果
     */
    long countMine(@Param("userId") Long userId);

    /**
     * 查询预约信息
     * @param offset 参数
     * @param pageSize 每页条数
     * @return 数据列表
     */
    List<ReservationEntity> selectPendingAudit(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计预约信息数量
     * @return 处理结果
     */
    long countPendingAudit();

    /**
     * 新增预约信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(ReservationEntity entity);

    /**
     * 更新预约信息
     * @param entity 参数
     * @return 处理结果
     */
    int updateAuditResult(ReservationEntity entity);

    /**
     * 取消预约信息
     * @param id 主键ID
     * @return 处理结果
     */
    int cancel(@Param("id") Long id);

    /**
     * 预约信息签到
     * @param id 主键ID
     * @return 处理结果
     */
    int checkIn(@Param("id") Long id);

    /**
     * 预约信息签退
     * @param id 主键ID
     * @return 处理结果
     */
    int checkOut(@Param("id") Long id);

    /**
     * 统计预约信息数量
     * @param labId 实验室ID
     * @param startTime 时间参数
     * @param endTime 时间参数
     * @return 处理结果
     */
    int countConflict(@Param("labId") Long labId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询预约信息
     * @param labId 实验室ID
     * @param reservationDate 预约日期
     * @return 数据列表
     */
    List<ReservationEntity> selectByLabAndDate(@Param("labId") Long labId,
    @Param("reservationDate") LocalDate reservationDate);

    /**
     * 统计预约信息数量
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @param labId 实验室ID
     * @return 处理结果
     */
    List<Map<String, Object>> countByReservationType(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("labId") Long labId);

    /**
     * 统计预约信息数量
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @param labId 实验室ID
     * @return 处理结果
     */
    List<Map<String, Object>> countByStatus(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("labId") Long labId);

    /**
     * 统计预约信息数量
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @param departmentId 部门ID
     * @param labId 实验室ID
     * @return 处理结果
     */
    List<Map<String, Object>> countByLabUsage(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("departmentId") Long departmentId,
    @Param("labId") Long labId);

    /**
     * 统计预约信息数量
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @param labId 实验室ID
     * @return 处理结果
     */
    List<Map<String, Object>> countByTimeDistribution(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("labId") Long labId);

    /**
     * 获取预约趋势统计数据
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @return 处理结果
     */
    List<Map<String, Object>> reservationTrend(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);

}

package com.nlt.mapper;

import com.nlt.domain.entity.ViolationRecordEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ViolationMapper {

    /**
     * 查询违规记录
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @param violationType 违规类型
     * @return 数据列表
     */
    List<ViolationRecordEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("userId") Long userId, @Param("reservationId") Long reservationId,
    @Param("violationType") Integer violationType);

    /**
     * 统计违规记录数量
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @param violationType 违规类型
     * @return 处理结果
     */
    long countPage(@Param("userId") Long userId, @Param("reservationId") Long reservationId,
    @Param("violationType") Integer violationType);

    /**
     * 查询违规记录
     * @param id 主键ID
     * @return 处理结果
     */
    ViolationRecordEntity selectById(@Param("id") Long id);

    /**
     * 查询我的违规记录
     * @param userId 用户ID
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @return 数据列表
     */
    List<ViolationRecordEntity> selectMine(@Param("userId") Long userId, @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计我的违规记录数量
     * @param userId 用户ID
     * @return 处理结果
     */
    long countMine(@Param("userId") Long userId);

    /**
     * 新增违规记录
     * @param entity 参数
     * @return 处理结果
     */
    int insert(ViolationRecordEntity entity);

    boolean existsByReservationAndType(@Param("reservationId") Long reservationId,
        @Param("violationType") Integer violationType);

    boolean existsByReservationId(@Param("reservationId") Long reservationId);

    /**
     * 删除违规记录
     * @param id 主键ID
     * @return 处理结果
     */
    int deleteById(@Param("id") Long id);

    /**
     * 按类型统计违规记录数量
     * @param startDate 日期参数
     * @param endDate 日期参数
     * @param departmentId 部门ID
     * @param violationType 违规类型
     * @return 处理结果
     */
    List<Map<String, Object>> countByType(@Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("departmentId") Long departmentId,
    @Param("violationType") Integer violationType);

}


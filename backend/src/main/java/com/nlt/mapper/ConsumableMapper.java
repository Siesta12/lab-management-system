package com.nlt.mapper;

import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.vo.statistics.export.ConsumableExportVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumableMapper {

    /**
     * 分页查询耗材信息
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param consumableName 耗材名称
     * @param consumableCode 耗材编码
     * @return 数据列表
     */
    List<ConsumableEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                      @Param("labId") Long labId, @Param("labType") String labType, @Param("consumableName") String consumableName,
                                      @Param("consumableCode") String consumableCode,
                                      @Param("departmentId") Long departmentId,
                                      @Param("status") Integer status);

    /**
     * 统计耗材信息数量
     * @param labId 实验室ID
     * @param consumableName 耗材名称
     * @param consumableCode 耗材编码
     * @return 总数
     */
    long countPage(@Param("labId") Long labId, @Param("labType") String labType, @Param("consumableName") String consumableName,
                   @Param("consumableCode") String consumableCode,
                   @Param("departmentId") Long departmentId,
                   @Param("status") Integer status);

    /**
     * 根据ID查询耗材信息
     * @param id 主键ID
     * @return 耗材实体
     */
    ConsumableEntity selectById(@Param("id") Long id);

    ConsumableEntity selectByIdForUpdate(@Param("id") Long id);

    List<ConsumableEntity> selectAvailableOptions(@Param("labId") Long labId);

    /**
     * 查询库存预警耗材列表
     * @return 数据列表
     */
    List<ConsumableEntity> selectWarningList(@Param("departmentId") Long departmentId);

    /**
     * 新增耗材信息
     * @param entity 耗材实体
     * @return 影响行数
     */
    int insert(ConsumableEntity entity);

    /**
     * 更新耗材信息
     * @param entity 耗材实体
     * @return 影响行数
     */
    int update(ConsumableEntity entity);

    /**
     * 更新耗材库存
     * @param id 主键ID
     * @param stockQuantity 库存数量
     * @return 影响行数
     */
    int updateStock(@Param("id") Long id, @Param("stockQuantity") Integer stockQuantity);

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 逻辑删除耗材信息
     * @param id 主键ID
     * @return 影响行数
     */
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


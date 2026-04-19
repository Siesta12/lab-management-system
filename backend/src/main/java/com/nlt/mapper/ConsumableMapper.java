package com.nlt.mapper;

import com.nlt.domain.entity.ConsumableEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumableMapper {

    /**
     * 查询耗材信息
     * @param offset 参数
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param consumableName 参数
     * @param consumableCode 参数
     * @return 数据列表
     */
    List<ConsumableEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("labId") Long labId, @Param("consumableName") String consumableName,
    @Param("consumableCode") String consumableCode);

    /**
     * 统计耗材信息数量
     * @param labId 实验室ID
     * @param consumableName 参数
     * @param consumableCode 参数
     * @return 处理结果
     */
    long countPage(@Param("labId") Long labId, @Param("consumableName") String consumableName,
    @Param("consumableCode") String consumableCode);

    /**
     * 查询耗材信息
     * @param id 主键ID
     * @return 处理结果
     */
    ConsumableEntity selectById(@Param("id") Long id);

    /**
     * 查询耗材信息
     * @return 数据列表
     */
    List<ConsumableEntity> selectWarningList();

    /**
     * 新增耗材信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(ConsumableEntity entity);

    /**
     * 更新耗材信息
     * @param entity 参数
     * @return 处理结果
     */
    int update(ConsumableEntity entity);

    /**
     * 更新耗材信息
     * @param id 主键ID
     * @param stockQuantity 参数
     * @return 处理结果
     */
    int updateStock(@Param("id") Long id, @Param("stockQuantity") Integer stockQuantity);

    /**
     * 处理耗材信息
     * @param id 主键ID
     * @return 处理结果
     */
    int softDelete(@Param("id") Long id);

}

package com.nlt.mapper;

import com.nlt.domain.entity.LabEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabMapper {

    /**
     * 查询实验室信息
     * @param offset 参数
     * @param pageSize 每页条数
     * @param labName 参数
     * @param labCode 参数
     * @param labType 参数
     * @param departmentId 部门ID
     * @param openStatus 参数
     * @param labStatus 参数
     * @return 数据列表
     */
    List<LabEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("labName") String labName, @Param("labCode") String labCode,
    @Param("labType") String labType, @Param("departmentId") Long departmentId,
    @Param("openStatus") Integer openStatus, @Param("labStatus") Integer labStatus);

    /**
     * 统计实验室信息数量
     * @param labName 参数
     * @param labCode 参数
     * @param labType 参数
     * @param departmentId 部门ID
     * @param openStatus 参数
     * @param labStatus 参数
     * @return 处理结果
     */
    long countPage(@Param("labName") String labName, @Param("labCode") String labCode,
    @Param("labType") String labType, @Param("departmentId") Long departmentId,
    @Param("openStatus") Integer openStatus, @Param("labStatus") Integer labStatus);

    /**
     * 查询实验室信息
     * @param id 主键ID
     * @return 处理结果
     */
    LabEntity selectById(@Param("id") Long id);

    /**
     * 查询实验室信息
     * @param openStatus 参数
     * @return 数据列表
     */
    List<LabEntity> selectOptions(@Param("openStatus") Integer openStatus);

    /**
     * 新增实验室信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(LabEntity entity);

    /**
     * 更新实验室信息
     * @param entity 参数
     * @return 处理结果
     */
    int update(LabEntity entity);

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param status 状态值
     * @return 处理结果
     */
    int updateOpenStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param status 状态值
     * @return 处理结果
     */
    int updateLabStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 处理实验室信息
     * @param id 主键ID
     * @return 处理结果
     */
    int softDelete(@Param("id") Long id);

    /**
     * 查询实验室信息
     * @param excludeLabId excludeLabID
     * @param participantCount 参数
     * @return 数据列表
     */
    List<LabEntity> selectRecommendationCandidates(@Param("excludeLabId") Long excludeLabId,
    @Param("participantCount") Integer participantCount);

}

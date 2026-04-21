package com.nlt.mapper;

import com.nlt.domain.entity.LabEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabMapper {

    /**
     * 查询实验室信息（分页）
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param labName 实验室名称
     * @param labCode 实验室编码
     * @param labType 实验室类型
     * @param departmentId 部门ID
     * @param openStatus 开放状态
     * @param labStatus 实验室状态
     * @return 数据列表
     */
    List<LabEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("labId") Long labId, @Param("labName") String labName, @Param("labCode") String labCode,
    @Param("labType") String labType, @Param("departmentId") Long departmentId,
    @Param("openStatus") Integer openStatus, @Param("labStatus") Integer labStatus);

    /**
     * 统计实验室信息数量
     * @param labName 实验室名称
     * @param labCode 实验室编码
     * @param labType 实验室类型
     * @param departmentId 部门ID
     * @param openStatus 开放状态
     * @param labStatus 实验室状态
     * @return 总数
     */
    long countPage(@Param("labId") Long labId, @Param("labName") String labName, @Param("labCode") String labCode,
    @Param("labType") String labType, @Param("departmentId") Long departmentId,
    @Param("openStatus") Integer openStatus, @Param("labStatus") Integer labStatus);

    /**
     * 根据ID查询实验室信息
     * @param id 主键ID
     * @return 实验室实体
     */
    LabEntity selectById(@Param("id") Long id);

    /**
     * 查询实验室选项列表
     * @param openStatus 开放状态
     * @return 数据列表
     */
    List<LabEntity> selectOptions(@Param("openStatus") Integer openStatus, @Param("departmentId") Long departmentId);

    /**
     * 新增实验室信息
     * @param entity 实验室实体
     * @return 影响行数
     */
    int insert(LabEntity entity);

    /**
     * 更新实验室信息
     * @param entity 实验室实体
     * @return 影响行数
     */
    int update(LabEntity entity);

    /**
     * 更新实验室开放状态
     * @param id 主键ID
     * @param status 状态值
     * @return 影响行数
     */
    int updateOpenStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新实验室状态
     * @param id 主键ID
     * @param status 状态值
     * @return 影响行数
     */
    int updateLabStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 软删除实验室信息
     * @param id 主键ID
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id);

    /**
     * 查询推荐候选实验室列表
     * @param excludeLabId 排除的实验室ID
     * @param participantCount 参与人数
     * @return 数据列表
     */
    List<LabEntity> selectRecommendationCandidates(@Param("excludeLabId") Long excludeLabId,
    @Param("participantCount") Integer participantCount);

}


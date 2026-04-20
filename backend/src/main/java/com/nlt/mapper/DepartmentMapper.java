package com.nlt.mapper;

import com.nlt.domain.entity.DepartmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepartmentMapper {

    /**
     * 分页查询部门信息
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param departmentName 部门名称
     * @param departmentCode 部门编码
     * @param status 状态值
     * @return 数据列表
     */
    List<DepartmentEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                      @Param("departmentName") String departmentName,
                                      @Param("departmentCode") String departmentCode,
                                      @Param("status") Integer status);

    /**
     * 统计部门信息数量
     * @param departmentName 部门名称
     * @param departmentCode 部门编码
     * @param status 状态值
     * @return 处理结果
     */
    long countPage(@Param("departmentName") String departmentName,
                   @Param("departmentCode") String departmentCode,
                   @Param("status") Integer status);

    /**
     * 根据ID查询部门信息
     * @param id 主键ID
     * @return 处理结果
     */
    DepartmentEntity selectById(@Param("id") Long id);

    /**
     * 查询部门选项列表
     * @return 数据列表
     */
    List<DepartmentEntity> selectOptions();

    /**
     * 新增部门信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(DepartmentEntity entity);

    /**
     * 更新部门信息
     * @param entity 参数
     * @return 处理结果
     */
    int update(DepartmentEntity entity);

    /**
     * 逻辑删除部门信息
     * @param id 主键ID
     * @return 处理结果
     */
    int softDelete(@Param("id") Long id);

}


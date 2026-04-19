package com.nlt.mapper;

import com.nlt.domain.entity.RoleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper {

    /**
     * 查询角色信息
     * @param offset 参数
     * @param pageSize 每页条数
     * @param roleName 参数
     * @param roleCode 参数
     * @param status 状态值
     * @return 数据列表
     */
    List<RoleEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("roleName") String roleName, @Param("roleCode") String roleCode,
    @Param("status") Integer status);

    /**
     * 统计角色信息数量
     * @param roleName 参数
     * @param roleCode 参数
     * @param status 状态值
     * @return 处理结果
     */
    long countPage(@Param("roleName") String roleName, @Param("roleCode") String roleCode,
    @Param("status") Integer status);

    /**
     * 查询角色信息
     * @param id 主键ID
     * @return 处理结果
     */
    RoleEntity selectById(@Param("id") Long id);

    /**
     * 查询角色信息
     * @return 数据列表
     */
    List<RoleEntity> selectOptions();

    /**
     * 查询角色信息
     * @param ids 参数
     * @return 数据列表
     */
    List<RoleEntity> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 新增角色信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(RoleEntity entity);

    /**
     * 更新角色信息
     * @param entity 参数
     * @return 处理结果
     */
    int update(RoleEntity entity);

    /**
     * 处理角色信息
     * @param id 主键ID
     * @return 处理结果
     */
    int softDelete(@Param("id") Long id);

}

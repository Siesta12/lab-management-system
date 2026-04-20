package com.nlt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper {

    /**
     * 查询用户角色映射
     * @param userId 用户ID
     * @return 数据列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户角色编码
     * @param userId 用户ID
     * @return 数据列表
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 删除用户角色映射
     * @param userId 用户ID
     * @return 处理结果
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 新增用户角色映射
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 处理结果
     */
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

}


package com.nlt.mapper;

import com.nlt.domain.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /**
     * 分页查询用户信息
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param username 用户名
     * @param realName 真实姓名
     * @param departmentId 部门ID
     * @param status 状态值
     * @return 数据列表
     */
    List<UserEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("username") String username, @Param("realName") String realName,
    @Param("departmentId") Long departmentId, @Param("status") Integer status);

    /**
     * 统计用户信息数量
     * @param username 用户名
     * @param realName 真实姓名
     * @param departmentId 部门ID
     * @param status 状态值
     * @return 处理结果
     */
    long countPage(@Param("username") String username, @Param("realName") String realName,
    @Param("departmentId") Long departmentId, @Param("status") Integer status);

    /**
     * 根据ID查询用户信息
     * @param id 主键ID
     * @return 处理结果
     */
    UserEntity selectById(@Param("id") Long id);

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 处理结果
     */
    UserEntity selectByUsername(@Param("username") String username);

    /**
     * 查询用户选项列表
     * @param status 状态值
     * @return 数据列表
     */
    List<UserEntity> selectOptions(@Param("status") Integer status);

    /**
     * 新增用户信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(UserEntity entity);

    /**
     * 更新用户信息
     * @param entity 参数
     * @return 处理结果
     */
    int update(UserEntity entity);

    /**
     * 更新用户个人资料
     * @param entity 参数
     * @return 处理结果
     */
    int updateProfile(UserEntity entity);

    /**
     * 更新用户密码
     * @param id 主键ID
     * @param password 密码
     * @return 处理结果
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 更新用户状态
     * @param id 主键ID
     * @param status 状态值
     * @return 处理结果
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新用户最后登录时间
     * @param id 主键ID
     * @return 处理结果
     */
    int updateLastLoginAt(@Param("id") Long id);

    /**
     * 软删除用户信息
     * @param id 主键ID
     * @return 处理结果
     */
    int softDelete(@Param("id") Long id);

}


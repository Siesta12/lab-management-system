package com.nlt.mapper;

import com.nlt.domain.entity.RoleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper {

    List<RoleEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("roleName") String roleName, @Param("roleCode") String roleCode,
    @Param("status") Integer status);

    long countPage(@Param("roleName") String roleName, @Param("roleCode") String roleCode,
    @Param("status") Integer status);

    RoleEntity selectById(@Param("id") Long id);

    List<RoleEntity> selectOptions();

    List<RoleEntity> selectByIds(@Param("ids") List<Long> ids);

    int insert(RoleEntity entity);

    int update(RoleEntity entity);

    int softDelete(@Param("id") Long id);

}


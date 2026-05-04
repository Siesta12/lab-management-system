package com.nlt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper {

    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    int deleteByUserId(@Param("userId") Long userId);

    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

}


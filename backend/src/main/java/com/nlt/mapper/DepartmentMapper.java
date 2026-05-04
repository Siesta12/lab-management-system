package com.nlt.mapper;

import com.nlt.domain.entity.DepartmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepartmentMapper {

    List<DepartmentEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                      @Param("departmentName") String departmentName,
                                      @Param("departmentCode") String departmentCode,
                                      @Param("status") Integer status);

    long countPage(@Param("departmentName") String departmentName,
                   @Param("departmentCode") String departmentCode,
                   @Param("status") Integer status);

    DepartmentEntity selectById(@Param("id") Long id);

    List<DepartmentEntity> selectOptions();

    int insert(DepartmentEntity entity);

    int update(DepartmentEntity entity);

    int softDelete(@Param("id") Long id);

}


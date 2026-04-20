package com.nlt.mapper;

import com.nlt.domain.entity.ClassPeriodEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClassPeriodMapper {

    List<ClassPeriodEntity> selectActiveList();

    ClassPeriodEntity selectById(@Param("id") Long id);
}



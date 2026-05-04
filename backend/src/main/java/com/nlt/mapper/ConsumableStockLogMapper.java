package com.nlt.mapper;

import com.nlt.domain.entity.ConsumableStockLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumableStockLogMapper {

    int insert(ConsumableStockLogEntity entity);

    ConsumableStockLogEntity selectById(@Param("id") Long id);

}

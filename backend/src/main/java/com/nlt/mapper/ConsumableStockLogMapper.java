package com.nlt.mapper;

import com.nlt.domain.entity.ConsumableStockLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumableStockLogMapper {

    /**
     * 新增耗材库存流水
     * @param entity 参数
     * @return 处理结果
     */
    int insert(ConsumableStockLogEntity entity);

    ConsumableStockLogEntity selectById(@Param("id") Long id);

}

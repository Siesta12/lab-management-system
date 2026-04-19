package com.nlt.mapper;

import com.nlt.domain.entity.LabOpenRuleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabOpenRuleMapper {

    /**
     * 查询实验室开放规则
     * @param labId 实验室ID
     * @param weekday 参数
     * @return 数据列表
     */
    List<LabOpenRuleEntity> selectList(@Param("labId") Long labId, @Param("weekday") Integer weekday);

    /**
     * 查询实验室开放规则
     * @param id 主键ID
     * @return 处理结果
     */
    LabOpenRuleEntity selectById(@Param("id") Long id);

    /**
     * 新增实验室开放规则
     * @param entity 参数
     * @return 处理结果
     */
    int insert(LabOpenRuleEntity entity);

    /**
     * 更新实验室开放规则
     * @param entity 参数
     * @return 处理结果
     */
    int update(LabOpenRuleEntity entity);

    /**
     * 删除实验室开放规则
     * @param id 主键ID
     * @return 处理结果
     */
    int deleteById(@Param("id") Long id);

}

package com.nlt.service;

import com.nlt.domain.dto.lab.LabOpenRuleSaveRequest;
import com.nlt.domain.entity.LabOpenRuleEntity;
import java.util.List;

public interface LabOpenRuleService {

    /**
     * 查询实验室开放规则列表
     * @param labId 实验室ID
     * @param weekday 参数
     * @return 数据列表
     */
    List<LabOpenRuleEntity> list(Long labId, Integer weekday);

    /**
     * 新增实验室开放规则
     * @param request 请求参数
     * @return 处理结果
     */
    LabOpenRuleEntity create(LabOpenRuleSaveRequest request);

    /**
     * 查询实验室开放规则
     * @param id 主键ID
     * @return 处理结果
     */
    LabOpenRuleEntity getById(Long id);

    /**
     * 更新实验室开放规则
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    LabOpenRuleEntity update(Long id, LabOpenRuleSaveRequest request);

    /**
     * 删除实验室开放规则
     * @param id 主键ID
     */
    void delete(Long id);

}

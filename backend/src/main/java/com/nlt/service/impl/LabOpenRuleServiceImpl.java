package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.lab.LabOpenRuleSaveRequest;
import com.nlt.domain.entity.LabOpenRuleEntity;
import com.nlt.mapper.LabOpenRuleMapper;
import com.nlt.service.LabOpenRuleService;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabOpenRuleServiceImpl implements LabOpenRuleService {

    private final LabOpenRuleMapper labOpenRuleMapper;

    /**
     * 查询实验室开放规则列表
     * @param labId 实验室ID
     * @param weekday 参数
     * @return 数据列表
     */
    @Override
    public List<LabOpenRuleEntity> list(Long labId, Integer weekday) {
        return labOpenRuleMapper.selectList(labId, weekday);
    }

    /**
     * 新增实验室开放规则
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LabOpenRuleEntity create(LabOpenRuleSaveRequest request) {
        LabOpenRuleEntity entity = new LabOpenRuleEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setStartTime(LocalTime.parse(request.getStartTime()));
        entity.setEndTime(LocalTime.parse(request.getEndTime()));
        if (entity.getAllowStudent() == null) {
            entity.setAllowStudent(1);
        }
        if (entity.getAllowTeacher() == null) {
            entity.setAllowTeacher(1);
        }
        if (entity.getMaxReservationHours() == null) {
            entity.setMaxReservationHours(4);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        labOpenRuleMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 查询实验室开放规则
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public LabOpenRuleEntity getById(Long id) {
        LabOpenRuleEntity entity = labOpenRuleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "实验室开放规则不存在");
        }
        return entity;
    }

    /**
     * 更新实验室开放规则
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LabOpenRuleEntity update(Long id, LabOpenRuleSaveRequest request) {
        LabOpenRuleEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        entity.setStartTime(LocalTime.parse(request.getStartTime()));
        entity.setEndTime(LocalTime.parse(request.getEndTime()));
        labOpenRuleMapper.update(entity);
        return getById(id);
    }

    /**
     * 删除实验室开放规则
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        labOpenRuleMapper.deleteById(id);
    }

}

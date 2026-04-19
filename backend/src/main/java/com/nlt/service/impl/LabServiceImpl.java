package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.LabMapper;
import com.nlt.service.LabService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabMapper labMapper;

    /**
     * 查询实验室信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labName 参数
     * @param labCode 参数
     * @param labType 参数
     * @param departmentId 部门ID
     * @param openStatus 参数
     * @param labStatus 参数
     * @return 分页数据
     */
    @Override
    public PageData<LabEntity> page(int pageNum, int pageSize, String labName, String labCode, String labType,
    Long departmentId, Integer openStatus, Integer labStatus) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
        labMapper.selectPage(offset, pageSize, labName, labCode, labType, departmentId, openStatus, labStatus),
        labMapper.countPage(labName, labCode, labType, departmentId, openStatus, labStatus),
        pageNum,
        pageSize
        );
    }

    /**
     * 新增实验室信息
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LabEntity create(LabSaveRequest request) {
        LabEntity entity = new LabEntity();
        BeanUtils.copyProperties(request, entity);
        if (entity.getOpenStatus() == null) {
            entity.setOpenStatus(1);
        }
        if (entity.getLabStatus() == null) {
            entity.setLabStatus(1);
        }
        labMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 查询实验室信息
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public LabEntity getById(Long id) {
        LabEntity entity = labMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "实验室不存在");
        }
        return entity;
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LabEntity update(Long id, LabSaveRequest request) {
        LabEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        labMapper.update(entity);
        return getById(id);
    }

    /**
     * 删除实验室信息
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        labMapper.softDelete(id);
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param status 状态值
     */
    @Override
    public void updateOpenStatus(Long id, Integer status) {
        getById(id);
        labMapper.updateOpenStatus(id, status);
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param status 状态值
     */
    @Override
    public void updateLabStatus(Long id, Integer status) {
        getById(id);
        labMapper.updateLabStatus(id, status);
    }

    /**
     * 处理实验室信息
     * @param openStatus 参数
     * @return 数据列表
     */
    @Override
    public List<OptionItem> options(Integer openStatus) {
        return labMapper.selectOptions(openStatus).stream()
        .map(item -> new OptionItem(item.getLabName(), item.getId()))
        .toList();
    }

}

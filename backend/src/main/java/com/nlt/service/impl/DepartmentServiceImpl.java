package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.department.DepartmentSaveRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.DepartmentMapper;
import com.nlt.service.DepartmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    /**
     * 查询部门信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param departmentName 参数
     * @param departmentCode 参数
     * @param status 状态值
     * @return 分页数据
     */
    @Override
    public PageData<DepartmentEntity> page(int pageNum, int pageSize, String departmentName, String departmentCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
                departmentMapper.selectPage(offset, pageSize, departmentName, departmentCode, status),
                departmentMapper.countPage(departmentName, departmentCode, status),
                pageNum,
                pageSize
        );
    }

    /**
     * 新增部门信息
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public DepartmentEntity create(DepartmentSaveRequest request) {
        DepartmentEntity entity = new DepartmentEntity();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        departmentMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 查询部门信息
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public DepartmentEntity getById(Long id) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "部门不存在");
        }
        return entity;
    }

    /**
     * 更新部门信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public DepartmentEntity update(Long id, DepartmentSaveRequest request) {
        DepartmentEntity entity = getById(id);
        BeanUtils.copyProperties(request, entity);
        departmentMapper.update(entity);
        return getById(id);
    }

    /**
     * 删除部门信息
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        departmentMapper.softDelete(id);
    }

    /**
     * 获取部门选项列表
     * @return 数据列表
     */
    @Override
    public List<OptionItem> options() {
        return departmentMapper.selectOptions().stream()
                .map(item -> new OptionItem(item.getDepartmentName(), item.getId()))
                .toList();
    }

}


package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.department.DepartmentSaveRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface DepartmentService {

    /**
     * 查询部门信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param departmentName 参数
     * @param departmentCode 参数
     * @param status 状态值
     * @return 分页数据
     */
    PageData<DepartmentEntity> page(int pageNum, int pageSize, String departmentName, String departmentCode, Integer status);

    /**
     * 新增部门信息
     * @param request 请求参数
     * @return 处理结果
     */
    DepartmentEntity create(DepartmentSaveRequest request);

    /**
     * 查询部门信息
     * @param id 主键ID
     * @return 处理结果
     */
    DepartmentEntity getById(Long id);

    /**
     * 更新部门信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    DepartmentEntity update(Long id, DepartmentSaveRequest request);

    /**
     * 删除部门信息
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 获取部门选项列表
     * @return 数据列表
     */
    List<OptionItem> options();

}


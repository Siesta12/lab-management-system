package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface LabService {

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
    public PageData<LabEntity> page(int pageNum, int pageSize, String labName, String labCode, String labType,
    Long departmentId, Integer openStatus, Integer labStatus);

    /**
     * 新增实验室信息
     * @param request 请求参数
     * @return 处理结果
     */
    LabEntity create(LabSaveRequest request);

    /**
     * 查询实验室信息
     * @param id 主键ID
     * @return 处理结果
     */
    LabEntity getById(Long id);

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    LabEntity update(Long id, LabSaveRequest request);

    /**
     * 删除实验室信息
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param status 状态值
     */
    void updateOpenStatus(Long id, Integer status);

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param status 状态值
     */
    void updateLabStatus(Long id, Integer status);

    /**
     * 处理实验室信息
     * @param openStatus 参数
     * @return 数据列表
     */
    List<OptionItem> options(Integer openStatus);

}

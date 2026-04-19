package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.device.DeviceSaveRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface DeviceService {

    /**
     * 查询设备信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param deviceName 参数
     * @param deviceCode 参数
     * @param status 状态值
     * @return 分页数据
     */
    PageData<DeviceEntity> page(int pageNum, int pageSize, Long labId, String deviceName, String deviceCode, Integer status);

    /**
     * 新增设备信息
     * @param request 请求参数
     * @return 处理结果
     */
    DeviceEntity create(DeviceSaveRequest request);

    /**
     * 处理设备信息
     * @param labId 实验室ID
     * @return 数据列表
     */
    List<OptionItem> options(Long labId);

    /**
     * 查询设备信息
     * @param id 主键ID
     * @return 处理结果
     */
    DeviceEntity getById(Long id);

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    DeviceEntity update(Long id, DeviceSaveRequest request);

    /**
     * 删除设备信息
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param status 状态值
     */
    void updateStatus(Long id, Integer status);

}

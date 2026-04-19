package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.device.DeviceSaveRequest;
import com.nlt.domain.entity.DeviceEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.DeviceMapper;
import com.nlt.service.DeviceService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

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
    @Override
    public PageData<DeviceEntity> page(int pageNum, int pageSize, Long labId, String deviceName, String deviceCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
        deviceMapper.selectPage(offset, pageSize, labId, deviceName, deviceCode, status),
        deviceMapper.countPage(labId, deviceName, deviceCode, status),
        pageNum,
        pageSize
        );
    }

    /**
     * 新增设备信息
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public DeviceEntity create(DeviceSaveRequest request) {
        DeviceEntity entity = toEntity(request);
        deviceMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 处理设备信息
     * @param labId 实验室ID
     * @return 数据列表
     */
    @Override
    public List<OptionItem> options(Long labId) {
        return deviceMapper.selectOptions(labId).stream()
        .map(item -> new OptionItem(item.getDeviceName(), item.getId()))
        .toList();
    }

    /**
     * 查询设备信息
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public DeviceEntity getById(Long id) {
        DeviceEntity entity = deviceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "设备不存在");
        }
        return entity;
    }

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public DeviceEntity update(Long id, DeviceSaveRequest request) {
        DeviceEntity entity = toEntity(request);
        entity.setId(id);
        getById(id);
        deviceMapper.update(entity);
        return getById(id);
    }

    /**
     * 删除设备信息
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        deviceMapper.softDelete(id);
    }

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param status 状态值
     */
    @Override
    public void updateStatus(Long id, Integer status) {
        getById(id);
        deviceMapper.updateStatus(id, status);
    }

    /**
     * 转换设备信息
     * @param request 请求参数
     * @return 处理结果
     */
    private DeviceEntity toEntity(DeviceSaveRequest request) {
        DeviceEntity entity = new DeviceEntity();
        BeanUtils.copyProperties(request, entity);
        if (request.getPurchaseDate() != null && !request.getPurchaseDate().isBlank()) {
            entity.setPurchaseDate(LocalDate.parse(request.getPurchaseDate()));
        }
        if (entity.getQuantity() == null) {
            entity.setQuantity(1);
        }
        if (entity.getAvailableQuantity() == null) {
            entity.setAvailableQuantity(entity.getQuantity());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        return entity;
    }

}

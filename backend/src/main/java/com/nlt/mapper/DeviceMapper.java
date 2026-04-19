package com.nlt.mapper;

import com.nlt.domain.entity.DeviceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceMapper {

    /**
     * 查询设备信息
     * @param offset 参数
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param deviceName 参数
     * @param deviceCode 参数
     * @param status 状态值
     * @return 数据列表
     */
    List<DeviceEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
    @Param("labId") Long labId, @Param("deviceName") String deviceName,
    @Param("deviceCode") String deviceCode, @Param("status") Integer status);

    /**
     * 统计设备信息数量
     * @param labId 实验室ID
     * @param deviceName 参数
     * @param deviceCode 参数
     * @param status 状态值
     * @return 处理结果
     */
    long countPage(@Param("labId") Long labId, @Param("deviceName") String deviceName,
    @Param("deviceCode") String deviceCode, @Param("status") Integer status);

    /**
     * 查询设备信息
     * @param id 主键ID
     * @return 处理结果
     */
    DeviceEntity selectById(@Param("id") Long id);

    /**
     * 查询设备信息
     * @param labId 实验室ID
     * @return 数据列表
     */
    List<DeviceEntity> selectOptions(@Param("labId") Long labId);

    /**
     * 新增设备信息
     * @param entity 参数
     * @return 处理结果
     */
    int insert(DeviceEntity entity);

    /**
     * 更新设备信息
     * @param entity 参数
     * @return 处理结果
     */
    int update(DeviceEntity entity);

    /**
     * 更新设备信息
     * @param id 主键ID
     * @param status 状态值
     * @return 处理结果
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 处理设备信息
     * @param id 主键ID
     * @return 处理结果
     */
    int softDelete(@Param("id") Long id);

}

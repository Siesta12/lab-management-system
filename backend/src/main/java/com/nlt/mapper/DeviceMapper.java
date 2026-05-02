package com.nlt.mapper;

import com.nlt.domain.entity.DeviceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceMapper {

    /**
     * 分页查询设备信息
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @param labId 实验室ID
     * @param deviceName 设备名称
     * @param deviceCode 设备编号
     * @param status 状态值
     * @return 数据列表
     */
    List<DeviceEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                  @Param("labId") Long labId, @Param("labType") String labType,
                                  @Param("deviceName") String deviceName, @Param("deviceCode") String deviceCode, @Param("status") Integer status,
                                  @Param("departmentId") Long departmentId);

    /**
     * 统计设备信息数量
     * @param labId 实验室ID
     * @param deviceName 设备名称
     * @param deviceCode 设备编号
     * @param status 状态值
     * @return 总数
     */
    long countPage(@Param("labId") Long labId, @Param("labType") String labType, @Param("deviceName") String deviceName,
                   @Param("deviceCode") String deviceCode, @Param("status") Integer status,
                   @Param("departmentId") Long departmentId);

    /**
     * 根据ID查询设备信息
     * @param id 主键ID
     * @return 设备实体
     */
    DeviceEntity selectById(@Param("id") Long id);

    /**
     * 查询设备选项列表
     * @param labId 实验室ID
     * @return 数据列表
     */
    List<DeviceEntity> selectOptions(@Param("labId") Long labId, @Param("departmentId") Long departmentId);

    List<DeviceEntity> selectWarningList(@Param("departmentId") Long departmentId);

    /**
     * 新增设备信息
     * @param entity 设备实体
     * @return 影响行数
     */
    int insert(DeviceEntity entity);

    /**
     * 更新设备信息
     * @param entity 设备实体
     * @return 影响行数
     */
    int update(DeviceEntity entity);

    /**
     * 更新设备状态
     * @param id 主键ID
     * @param status 状态值
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 软删除设备信息
     * @param id 主键ID
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id);

}

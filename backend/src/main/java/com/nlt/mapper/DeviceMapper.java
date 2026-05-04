package com.nlt.mapper;

import com.nlt.domain.entity.DeviceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceMapper {

    List<DeviceEntity> selectPage(
        @Param("offset") int offset,
        @Param("pageSize") int pageSize,
        @Param("labId") Long labId,
        @Param("labType") String labType,
        @Param("deviceName") String deviceName,
        @Param("deviceCode") String deviceCode,
        @Param("status") Integer status,
        @Param("departmentId") Long departmentId
    );

    long countPage(
        @Param("labId") Long labId,
        @Param("labType") String labType,
        @Param("deviceName") String deviceName,
        @Param("deviceCode") String deviceCode,
        @Param("status") Integer status,
        @Param("departmentId") Long departmentId
    );

    DeviceEntity selectById(@Param("id") Long id);

    List<DeviceEntity> selectOptions(@Param("labId") Long labId, @Param("departmentId") Long departmentId);

    List<DeviceEntity> selectWarningList(@Param("departmentId") Long departmentId);

    int insert(DeviceEntity entity);

    long countByDeviceCode(@Param("deviceCode") String deviceCode);

    int update(DeviceEntity entity);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int softDelete(@Param("id") Long id);
}

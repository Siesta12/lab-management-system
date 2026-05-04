package com.nlt.mapper;

import com.nlt.domain.entity.DeviceRepairEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceRepairMapper {

    List<DeviceRepairEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                        @Param("labId") Long labId, @Param("labType") String labType, @Param("deviceId") Long deviceId,
                                        @Param("status") Integer status, @Param("departmentId") Long departmentId,
                                        @Param("applicantUserId") Long applicantUserId);

    long countPage(@Param("labId") Long labId, @Param("labType") String labType, @Param("deviceId") Long deviceId,
                   @Param("status") Integer status, @Param("departmentId") Long departmentId,
                   @Param("applicantUserId") Long applicantUserId);

    long countActiveByDeviceId(@Param("deviceId") Long deviceId, @Param("excludeId") Long excludeId);

    DeviceRepairEntity selectById(@Param("id") Long id);

    int insert(DeviceRepairEntity entity);

    int updateStatus(DeviceRepairEntity entity);
}

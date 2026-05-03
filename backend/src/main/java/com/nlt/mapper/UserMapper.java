package com.nlt.mapper;

import com.nlt.domain.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    List<UserEntity> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
        @Param("userNo") String userNo, @Param("realName") String realName,
        @Param("departmentId") Long departmentId, @Param("roleCode") String roleCode, @Param("status") Integer status);

    long countPage(@Param("userNo") String userNo, @Param("realName") String realName,
        @Param("departmentId") Long departmentId, @Param("roleCode") String roleCode, @Param("status") Integer status);

    UserEntity selectById(@Param("id") Long id);

    UserEntity selectCredentialById(@Param("id") Long id);

    UserEntity selectByUserNo(@Param("userNo") String userNo);

    UserEntity selectCredentialByUserNo(@Param("userNo") String userNo);

    UserEntity selectByPhone(@Param("phone") String phone);

    UserEntity selectByEmail(@Param("email") String email);

    List<UserEntity> selectOptions(@Param("status") Integer status);

    List<UserEntity> selectTeacherOptions(@Param("departmentId") Long departmentId);

    int insert(UserEntity entity);

    int update(UserEntity entity);

    int updateProfile(UserEntity entity);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateLastLoginAt(@Param("id") Long id);

    int adjustCreditAndViolation(@Param("id") Long id,
        @Param("scoreDelta") int scoreDelta,
        @Param("violationDelta") int violationDelta);

    int updateNormalReservationStreak(@Param("id") Long id, @Param("normalReservationStreak") int normalReservationStreak);

    int resetNormalReservationStreak(@Param("id") Long id);

    int recoverCreditScore();

    int softDelete(@Param("id") Long id);
}

package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.user.PasswordResetRequest;
import com.nlt.domain.dto.user.PasswordUpdateRequest;
import com.nlt.domain.dto.user.UserCreateRequest;
import com.nlt.domain.dto.user.UserProfileUpdateRequest;
import com.nlt.domain.dto.user.UserUpdateRequest;
import com.nlt.domain.entity.UserEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.domain.vo.user.UserVO;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    /**
     * 查询用户信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param username 参数
     * @param realName 参数
     * @param departmentId 部门ID
     * @param status 状态值
     * @return 分页数据
     */
    @Override
    public PageData<UserVO> page(int pageNum, int pageSize, String username, String realName, Long departmentId, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        List<UserVO> list = userMapper.selectPage(offset, pageSize, username, realName, departmentId, status)
        .stream().map(this::toVo).toList();
        return new PageData<>(list, userMapper.countPage(username, realName, departmentId, status), pageNum, pageSize);
    }

    /**
     * 新增用户信息
     * @param request 请求参数
     * @return 处理结果
     */
    @Transactional
    @Override
    public UserVO create(UserCreateRequest request) {
        UserEntity entity = new UserEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setCreditScore(100);
        entity.setViolationCount(0);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        userMapper.insert(entity);
        rebuildUserRoles(entity.getId(), request.getRoleIds());
        return getById(entity.getId());
    }

    /**
     * 查询用户信息
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public UserVO getById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVo(entity);
    }

    /**
     * 更新用户信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Transactional
    @Override
    public UserVO update(Long id, UserUpdateRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        BeanUtils.copyProperties(request, entity);
        userMapper.update(entity);
        rebuildUserRoles(id, request.getRoleIds());
        return getById(id);
    }

    /**
     * 处理用户信息
     * @param status 状态值
     * @return 数据列表
     */
    @Override
    public List<OptionItem> options(Integer status) {
        return userMapper.selectOptions(status).stream()
        .map(item -> new OptionItem(item.getRealName(), item.getId()))
        .toList();
    }

    /**
     * 删除用户信息
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        userMapper.softDelete(id);
    }

    /**
     * 重置用户信息
     * @param id 主键ID
     * @param request 请求参数
     */
    @Override
    public void resetPassword(Long id, PasswordResetRequest request) {
        getById(id);
        userMapper.updatePassword(id, request.getNewPassword());
    }

    /**
     * 更新用户信息
     * @param id 主键ID
     * @param status 状态值
     */
    @Override
    public void updateStatus(Long id, Integer status) {
        getById(id);
        userMapper.updateStatus(id, status);
    }

    /**
     * 获取当前登录用户信息
     * @param userId 用户ID
     * @return 处理结果
     */
    @Override
    public UserVO currentUser(Long userId) {
        return getById(userId);
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public UserVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        BeanUtils.copyProperties(request, entity);
        userMapper.updateProfile(entity);
        return getById(userId);
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param request 请求参数
     */
    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!entity.getPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        userMapper.updatePassword(userId, request.getNewPassword());
    }

    /**
     * 转换用户信息
     * @param entity 参数
     * @return 处理结果
     */
    private UserVO toVo(UserEntity entity) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setRoleIds(new ArrayList<>(userRoleMapper.selectRoleIdsByUserId(entity.getId())));
        return vo;
    }

    /**
     * 处理用户信息
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    private void rebuildUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            userRoleMapper.insert(userId, roleId);
        }
    }

}

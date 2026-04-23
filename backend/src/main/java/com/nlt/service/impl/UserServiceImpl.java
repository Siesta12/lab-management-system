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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    @Override
    public PageData<UserVO> page(int pageNum, int pageSize, String username, String realName, Long departmentId, String roleCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        List<UserVO> list = userMapper.selectPage(offset, pageSize, username, realName, departmentId, roleCode, status)
            .stream()
            .map(this::toVo)
            .toList();
        return new PageData<>(list, userMapper.countPage(username, realName, departmentId, roleCode, status), pageNum, pageSize);
    }

    @Transactional
    @Override
    public UserVO create(UserCreateRequest request) {
        ensureUsernameAvailable(request.getUsername(), null);

        UserEntity entity = new UserEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setCreditScore(100);
        entity.setViolationCount(0);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "用户名已存在");
        }
        rebuildUserRoles(entity.getId(), request.getRoleIds());
        return getById(entity.getId());
    }

    @Override
    public UserVO getById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVo(entity);
    }

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

    @Override
    public List<OptionItem> options(Integer status) {
        return userMapper.selectOptions(status).stream()
            .map(item -> new OptionItem(item.getRealName(), item.getId()))
            .toList();
    }

    @Override
    public void delete(Long id) {
        getById(id);
        userMapper.softDelete(id);
    }

    @Override
    public void resetPassword(Long id, PasswordResetRequest request) {
        getById(id);
        userMapper.updatePassword(id, request.getNewPassword());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        getById(id);
        userMapper.updateStatus(id, status);
    }

    @Override
    public UserVO currentUser(Long userId) {
        return getById(userId);
    }

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

    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!entity.getPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }
        userMapper.updatePassword(userId, request.getNewPassword());
    }

    private UserVO toVo(UserEntity entity) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setRoleIds(new ArrayList<>(userRoleMapper.selectRoleIdsByUserId(entity.getId())));
        return vo;
    }

    private void rebuildUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            userRoleMapper.insert(userId, roleId);
        }
    }

    private void ensureUsernameAvailable(String username, Long currentUserId) {
        UserEntity existing = userMapper.selectByUsername(username);
        if (existing != null && (currentUserId == null || !existing.getId().equals(currentUserId))) {
            throw new BusinessException(400, "用户名已存在");
        }
    }
}

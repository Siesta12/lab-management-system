package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.lab.LabSaveRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.UserEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.UserMapper;
import com.nlt.service.LabService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabMapper labMapper;

    private final UserMapper userMapper;

    /**
     * 查询实验室信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param labName 实验室名称
     * @param labCode 实验室编号
     * @param labType 实验室类型
     * @param departmentId 学院ID
     * @param openStatus 开放状态
     * @param labStatus 实验室状态
     * @param currentUserId 当前用户ID
     * @param currentRoleCodes 当前用户角色编码
     * @return 分页数据
     */
    @Override
    public PageData<LabEntity> page(int pageNum, int pageSize, String labName, String labCode, String labType,
        Long departmentId, Integer openStatus, Integer labStatus, Long currentUserId, List<String> currentRoleCodes) {
        Long allowedDepartmentId = resolveAllowedDepartmentId(currentUserId, currentRoleCodes, departmentId);
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
            labMapper.selectPage(offset, pageSize, labName, labCode, labType, allowedDepartmentId, openStatus, labStatus),
            labMapper.countPage(labName, labCode, labType, allowedDepartmentId, openStatus, labStatus),
            pageNum,
            pageSize
        );
    }

    /**
     * 新增实验室信息
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LabEntity create(LabSaveRequest request) {
        LabEntity entity = new LabEntity();
        BeanUtils.copyProperties(request, entity);
        if (entity.getOpenStatus() == null) {
            entity.setOpenStatus(1);
        }
        if (entity.getLabStatus() == null) {
            entity.setLabStatus(1);
        }
        labMapper.insert(entity);
        return getById(entity.getId(), null, List.of("ADMIN"));
    }

    /**
     * 查询实验室信息
     * @param id 主键ID
     * @param currentUserId 当前用户ID
     * @param currentRoleCodes 当前用户角色编码
     * @return 处理结果
     */
    @Override
    public LabEntity getById(Long id, Long currentUserId, List<String> currentRoleCodes) {
        LabEntity entity = labMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "实验室不存在");
        }
        validateDepartmentAccess(entity, currentUserId, currentRoleCodes);
        return entity;
    }

    /**
     * 更新实验室信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public LabEntity update(Long id, LabSaveRequest request) {
        LabEntity entity = getById(id, null, List.of("ADMIN"));
        BeanUtils.copyProperties(request, entity);
        labMapper.update(entity);
        return getById(id, null, List.of("ADMIN"));
    }

    /**
     * 删除实验室信息
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id, null, List.of("ADMIN"));
        labMapper.softDelete(id);
    }

    /**
     * 更新实验室开放状态
     * @param id 主键ID
     * @param status 状态值
     */
    @Override
    public void updateOpenStatus(Long id, Integer status) {
        getById(id, null, List.of("ADMIN"));
        labMapper.updateOpenStatus(id, status);
    }

    /**
     * 更新实验室运行状态
     * @param id 主键ID
     * @param status 状态值
     */
    @Override
    public void updateLabStatus(Long id, Integer status) {
        getById(id, null, List.of("ADMIN"));
        labMapper.updateLabStatus(id, status);
    }

    /**
     * 查询实验室选项
     * @param openStatus 开放状态
     * @param currentUserId 当前用户ID
     * @param currentRoleCodes 当前用户角色编码
     * @return 数据列表
     */
    @Override
    public List<OptionItem> options(Integer openStatus, Long currentUserId, List<String> currentRoleCodes) {
        Long allowedDepartmentId = resolveAllowedDepartmentId(currentUserId, currentRoleCodes, null);
        return labMapper.selectOptions(openStatus).stream()
            .filter(item -> allowedDepartmentId == null || allowedDepartmentId.equals(item.getDepartmentId()))
            .map(item -> new OptionItem(item.getLabName(), item.getId()))
            .toList();
    }

    private Long resolveAllowedDepartmentId(Long currentUserId, List<String> currentRoleCodes, Long requestedDepartmentId) {
        if (!isDepartmentRestrictedRole(currentRoleCodes)) {
            return requestedDepartmentId;
        }

        UserEntity currentUser = loadCurrentUser(currentUserId);
        if (currentUser.getDepartmentId() == null) {
            throw new BusinessException(403, "当前用户没有关联院系，无法查看实验室");
        }

        return currentUser.getDepartmentId();
    }

    private void validateDepartmentAccess(LabEntity entity, Long currentUserId, List<String> currentRoleCodes) {
        if (!isDepartmentRestrictedRole(currentRoleCodes)) {
            return;
        }

        UserEntity currentUser = loadCurrentUser(currentUserId);
        if (currentUser.getDepartmentId() == null) {
            throw new BusinessException(403, "当前用户没有关联院系，无法查看实验室");
        }

        if (!currentUser.getDepartmentId().equals(entity.getDepartmentId())) {
            throw new BusinessException(403, "只能查看所属院系的实验室");
        }
    }

    private UserEntity loadCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        UserEntity currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return currentUser;
    }

    private boolean isDepartmentRestrictedRole(List<String> currentRoleCodes) {
        if (currentRoleCodes == null || currentRoleCodes.isEmpty()) {
            return false;
        }

        return currentRoleCodes.stream().anyMatch(roleCode ->
            "STUDENT".equalsIgnoreCase(roleCode)
                || "ROLE_STUDENT".equalsIgnoreCase(roleCode)
                || "TEACHER".equalsIgnoreCase(roleCode)
                || "ROLE_TEACHER".equalsIgnoreCase(roleCode));
    }
}


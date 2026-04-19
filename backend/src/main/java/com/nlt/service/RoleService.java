package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.role.RoleSaveRequest;
import com.nlt.domain.entity.RoleEntity;
import com.nlt.domain.vo.common.OptionItem;
import java.util.List;

public interface RoleService {

    /**
     * 查询角色信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param roleName 参数
     * @param roleCode 参数
     * @param status 状态值
     * @return 分页数据
     */
    PageData<RoleEntity> page(int pageNum, int pageSize, String roleName, String roleCode, Integer status);

    /**
     * 新增角色信息
     * @param request 请求参数
     * @return 处理结果
     */
    RoleEntity create(RoleSaveRequest request);

    /**
     * 查询角色信息
     * @param id 主键ID
     * @return 处理结果
     */
    RoleEntity getById(Long id);

    /**
     * 更新角色信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    RoleEntity update(Long id, RoleSaveRequest request);

    /**
     * 删除角色信息
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 处理角色信息
     * @return 数据列表
     */
    List<OptionItem> options();

}

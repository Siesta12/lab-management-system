package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.role.RoleSaveRequest;
import com.nlt.domain.entity.RoleEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param roleName 角色名称
     * @param roleCode 角色编码
     * @param status 状态
     * @return 分页数据
     */
    @GetMapping
    public ApiResponse<PageData<RoleEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String roleName,
        @RequestParam(required = false) String roleCode,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(roleService.page(pageNum, pageSize, roleName, roleCode, status));
    }

    /**
     * 创建角色
     * @param request 请求参数
     * @return 创建后的角色信息
     */
    @PostMapping
    public ApiResponse<RoleEntity> create(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.created(roleService.create(request));
    }

    /**
     * 获取角色选项列表
     * @return 角色选项列表
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(roleService.options());
    }

    /**
     * 根据ID查询角色详情
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RoleEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    /**
     * 更新角色信息
     * @param id 角色ID
     * @param request 请求参数
     * @return 更新后的角色信息
     */
    @PutMapping("/{id}")
    public ApiResponse<RoleEntity> update(@PathVariable Long id,
        @Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    /**
     * 删除角色
     * @param id 角色ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

}


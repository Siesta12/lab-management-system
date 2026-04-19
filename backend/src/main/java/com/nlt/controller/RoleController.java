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
     * 查询角色信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param roleName 参数
     * @param roleCode 参数
     * @param status 状态值
     * @return 响应结果
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
     * 新增角色信息
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<RoleEntity> create(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    /**
     * 处理角色信息
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(roleService.options());
    }

    /**
     * 查询角色信息
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<RoleEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    /**
     * 更新角色信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public ApiResponse<RoleEntity> update(@PathVariable Long id,
        @Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    /**
     * 删除角色信息
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

}

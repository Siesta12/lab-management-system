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

    @GetMapping
    public ApiResponse<PageData<RoleEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String roleName,
        @RequestParam(required = false) String roleCode,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(roleService.page(pageNum, pageSize, roleName, roleCode, status));
    }

    @PostMapping
    public ApiResponse<RoleEntity> create(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.created(roleService.create(request));
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(roleService.options());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleEntity> update(@PathVariable Long id,
        @Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

}


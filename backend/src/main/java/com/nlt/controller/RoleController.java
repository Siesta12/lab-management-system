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
     * 鏌ヨ瑙掕壊淇℃伅鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param roleName 鍙傛暟
     * @param roleCode 鍙傛暟
     * @param status 鐘舵€佸€?
     * @return 鍝嶅簲缁撴灉
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
     * 鏂板瑙掕壊淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<RoleEntity> create(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.created(roleService.create(request));
    }

    /**
     * 澶勭悊瑙掕壊淇℃伅
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(roleService.options());
    }

    /**
     * 鏌ヨ瑙掕壊淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<RoleEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    /**
     * 鏇存柊瑙掕壊淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/{id}")
    public ApiResponse<RoleEntity> update(@PathVariable Long id,
        @Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    /**
     * 鍒犻櫎瑙掕壊淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

}

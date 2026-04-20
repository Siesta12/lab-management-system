package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.dto.department.DepartmentSaveRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 分页查询部门列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param departmentName 部门名称
     * @param departmentCode 部门编码
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<PageData<DepartmentEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String departmentName,
        @RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(departmentService.page(pageNum, pageSize, departmentName, departmentCode, status));
    }

    /**
     * 创建部门
     * @param request 创建请求参数
     * @return 创建的部门信息
     */
    @PostMapping
    public ApiResponse<DepartmentEntity> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.created(departmentService.create(request));
    }

    /**
     * 获取部门选项列表
     * @return 部门选项列表
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(departmentService.options());
    }

    /**
     * 根据ID查询部门详情
     * @param id 部门ID
     * @return 部门详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DepartmentEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    /**
     * 更新部门信息
     * @param id 部门ID
     * @param request 更新请求参数
     * @return 更新后的部门信息
     */
    @PutMapping("/{id}")
    public ApiResponse<DepartmentEntity> update(@PathVariable Long id,
        @Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    /**
     * 删除部门
     * @param id 部门ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }

}


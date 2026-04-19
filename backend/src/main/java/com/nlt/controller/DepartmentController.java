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
     * 查询部门信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param departmentName 参数
     * @param departmentCode 参数
     * @param status 状态值
     * @return 响应结果
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
     * 新增部门信息
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<DepartmentEntity> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.create(request));
    }

    /**
     * 处理部门信息
     * @return 响应结果
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(departmentService.options());
    }

    /**
     * 查询部门信息
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<DepartmentEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    /**
     * 更新部门信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public ApiResponse<DepartmentEntity> update(@PathVariable Long id,
        @Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    /**
     * 删除部门信息
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }

}

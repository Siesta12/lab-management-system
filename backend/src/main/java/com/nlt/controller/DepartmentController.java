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

    @GetMapping
    public ApiResponse<PageData<DepartmentEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String departmentName,
        @RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) Integer status) {
        return ApiResponse.success(departmentService.page(pageNum, pageSize, departmentName, departmentCode, status));
    }

    @PostMapping
    public ApiResponse<DepartmentEntity> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.created(departmentService.create(request));
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(departmentService.options());
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentEntity> update(@PathVariable Long id,
        @Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }

}


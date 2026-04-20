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
     * 鏌ヨ閮ㄩ棬淇℃伅鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param departmentName 鍙傛暟
     * @param departmentCode 鍙傛暟
     * @param status 鐘舵€佸€?
     * @return 鍝嶅簲缁撴灉
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
     * 鏂板閮ㄩ棬淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<DepartmentEntity> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.created(departmentService.create(request));
    }

    /**
     * 澶勭悊閮ㄩ棬淇℃伅
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.success(departmentService.options());
    }

    /**
     * 鏌ヨ閮ㄩ棬淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<DepartmentEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    /**
     * 鏇存柊閮ㄩ棬淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/{id}")
    public ApiResponse<DepartmentEntity> update(@PathVariable Long id,
        @Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    /**
     * 鍒犻櫎閮ㄩ棬淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }

}

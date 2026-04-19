package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.domain.dto.lab.LabOpenRuleSaveRequest;
import com.nlt.domain.entity.LabOpenRuleEntity;
import com.nlt.service.LabOpenRuleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lab-open-rules")
@RequiredArgsConstructor
public class LabOpenRuleController {

    private final LabOpenRuleService labOpenRuleService;

    /**
     * 查询实验室开放规则列表
     * @param labId 实验室ID
     * @param weekday 参数
     * @return 响应结果
     */
    @GetMapping
    public ApiResponse<List<LabOpenRuleEntity>> list(@RequestParam(required = false) Long labId,
        @RequestParam(required = false) Integer weekday) {
        return ApiResponse.success(labOpenRuleService.list(labId, weekday));
    }

    /**
     * 新增实验室开放规则
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping
    public ApiResponse<LabOpenRuleEntity> create(@Valid @RequestBody LabOpenRuleSaveRequest request) {
        return ApiResponse.success(labOpenRuleService.create(request));
    }

    /**
     * 查询实验室开放规则
     * @param id 主键ID
     * @return 响应结果
     */
    @GetMapping("/{id}")
    public ApiResponse<LabOpenRuleEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(labOpenRuleService.getById(id));
    }

    /**
     * 更新实验室开放规则
     * @param id 主键ID
     * @param request 请求参数
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public ApiResponse<LabOpenRuleEntity> update(@PathVariable Long id,
        @Valid @RequestBody LabOpenRuleSaveRequest request) {
        return ApiResponse.success(labOpenRuleService.update(id, request));
    }

    /**
     * 删除实验室开放规则
     * @param id 主键ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labOpenRuleService.delete(id);
        return ApiResponse.success();
    }

}

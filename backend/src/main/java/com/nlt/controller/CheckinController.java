package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.checkin.CheckinSubmitRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.service.DepartmentService;
import com.nlt.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final LabService labService;
    private final DepartmentService departmentService;

    @PostMapping("/submit")
    public ApiResponse<Void> submit(@Valid @RequestBody CheckinSubmitRequest request) {
        LabEntity lab = resolveLab(request.getLabIdentifier());
        DepartmentEntity department = lab.getDepartmentId() == null ? null : departmentService.getById(lab.getDepartmentId());
        System.out.println("实验室签到提交");
        System.out.println("labIdentifier=" + request.getLabIdentifier());
        System.out.println("实验室ID=" + lab.getId());
        System.out.println("实验室名称=" + lab.getLabName());
        System.out.println("实验室编号=" + lab.getLabCode());
        System.out.println("实验室位置=" + lab.getBuildingName() + " / " + lab.getRoomNo());
        System.out.println("所属学院=" + (department == null ? lab.getDepartmentId() : department.getDepartmentName()));
        System.out.println("手机定位 lat=" + request.getLatitude() + ", lng=" + request.getLongitude() + ", accuracy=" + request.getAccuracy());
        System.out.println("capturedAt=" + request.getCapturedAt());
        System.out.println("userAgent=" + request.getUserAgent());

        return ApiResponse.success();
    }

    private LabEntity resolveLab(String identifier) {
        try {
            if (identifier.matches("\\d+")) {
                return labService.getById(Long.parseLong(identifier));
            }
        } catch (Exception ignored) {
            // Fallback to code lookup below.
        }

        var page = labService.page(1, 1, null, null, identifier, null, null, null, null);
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            throw new BusinessException(404, "实验室不存在: " + identifier);
        }
        return page.getList().get(0);
    }
}

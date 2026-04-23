package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.checkin.CheckinSubmitRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.checkin.CheckinResultVo;
import com.nlt.service.CheckinService;
import com.nlt.service.LabService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final LabService labService;
    private final TokenService tokenService;
    private final CheckinService checkinService;

    @PostMapping("/submit")
    public ApiResponse<CheckinResultVo> submit(@Valid @RequestBody CheckinSubmitRequest request,
        HttpServletRequest servletRequest) {
        LabEntity lab = resolveLab(request.getLabIdentifier());
        Long currentUserId = tokenService.getCurrentUserId(servletRequest);
        return ApiResponse.success(checkinService.submit(request, lab, currentUserId));
    }

    private LabEntity resolveLab(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new BusinessException(400, "实验室编号不能为空");
        }

        try {
            if (identifier.matches("\\d+")) {
                LabEntity lab = labService.getById(Long.parseLong(identifier));
                if (lab != null) {
                    return lab;
                }
            }
        } catch (Exception ignored) {
            // Fall back to code lookup below.
        }

        var page = labService.page(1, 1, null, null, identifier, null, null, null, null);
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            throw new BusinessException(404, "实验室不存在: " + identifier);
        }
        return page.getList().get(0);
    }
}

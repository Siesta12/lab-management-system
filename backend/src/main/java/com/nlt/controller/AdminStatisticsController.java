package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.domain.dto.statistics.AdminStatisticsQuery;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ConsumableStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.CreditStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.DeviceStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.LabUsageStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OverviewVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ReservationStatsVo;
import com.nlt.service.AdminStatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/statistics/admin")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/options")
    public ApiResponse<OptionsVo> options() {
        return ApiResponse.success(adminStatisticsService.options());
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewVo> overview(AdminStatisticsQuery query) {
        return ApiResponse.success(adminStatisticsService.overview(query));
    }

    @GetMapping("/reservations")
    public ApiResponse<ReservationStatsVo> reservations(AdminStatisticsQuery query) {
        return ApiResponse.success(adminStatisticsService.reservations(query));
    }

    @GetMapping("/labs/usage")
    public ApiResponse<LabUsageStatsVo> labUsage(AdminStatisticsQuery query) {
        return ApiResponse.success(adminStatisticsService.labUsage(query));
    }

    @GetMapping("/devices")
    public ApiResponse<DeviceStatsVo> devices(AdminStatisticsQuery query) {
        return ApiResponse.success(adminStatisticsService.devices(query));
    }

    @GetMapping("/consumables")
    public ApiResponse<ConsumableStatsVo> consumables(AdminStatisticsQuery query) {
        return ApiResponse.success(adminStatisticsService.consumables(query));
    }

    @GetMapping("/credit")
    public ApiResponse<CreditStatsVo> credit(AdminStatisticsQuery query) {
        return ApiResponse.success(adminStatisticsService.credit(query));
    }

    @GetMapping("/export")
    public void export(@RequestParam String exportType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String labType,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Integer reservationType,
        HttpServletResponse response) {
        AdminStatisticsQuery query = new AdminStatisticsQuery();
        query.setExportType(exportType);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setLabId(labId);
        query.setLabType(labType);
        query.setStatus(status);
        query.setReservationType(reservationType);
        adminStatisticsService.export(query, response);
    }
}

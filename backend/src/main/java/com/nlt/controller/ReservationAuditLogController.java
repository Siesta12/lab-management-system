package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.domain.entity.ReservationAuditLogEntity;
import com.nlt.service.ReservationAuditLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReservationAuditLogController {

    private final ReservationAuditLogService reservationAuditLogService;

    @GetMapping("/reservation-audit-logs")
    public ApiResponse<PageData<ReservationAuditLogEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long reservationId,
        @RequestParam(required = false) Long auditUserId) {
        return ApiResponse.success(reservationAuditLogService.page(pageNum, pageSize, reservationId, auditUserId));
    }

    @GetMapping("/reservations/{id}/audit-logs")
    public ApiResponse<List<ReservationAuditLogEntity>> byReservation(@PathVariable Long id) {
        return ApiResponse.success(reservationAuditLogService.byReservationId(id));
    }

    @GetMapping("/reservation-audit-logs/{id}")
    public ApiResponse<ReservationAuditLogEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(reservationAuditLogService.getById(id));
    }

}


package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservationCheckInRequest {

    @NotNull(message = "实验室不能为空")
    private Long labId;

    @NotNull(message = "定位纬度不能为空")
    private Double latitude;

    @NotNull(message = "定位经度不能为空")
    private Double longitude;

    private Double accuracy;

    private LocalDateTime capturedAt;

    private String userAgent;
}

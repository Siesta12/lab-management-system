package com.nlt.domain.dto.reservation;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 扫码签到请求参数。
 */
@Data
public class ReservationCheckInRequest {

    /**
     * 当前扫码实验室 ID。
     */
    @NotNull(message = "实验室不能为空")
    private Long labId;

    /**
     * 用户当前纬度。
     */
    @NotNull(message = "定位纬度不能为空")
    private Double latitude;

    /**
     * 用户当前经度。
     */
    @NotNull(message = "定位经度不能为空")
    private Double longitude;

    /**
     * 浏览器定位精度，单位米。
     */
    private Double accuracy;

    /**
     * 前端采集定位的时间。
     */
    private LocalDateTime capturedAt;

    /**
     * 前端上报的 User-Agent，便于排查扫码端环境。
     */
    private String userAgent;
}

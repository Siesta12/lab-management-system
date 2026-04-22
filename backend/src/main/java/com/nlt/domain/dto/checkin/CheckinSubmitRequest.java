package com.nlt.domain.dto.checkin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckinSubmitRequest {

    @NotBlank
    private String labIdentifier;

    private Double latitude;

    private Double longitude;

    private Double accuracy;

    private String capturedAt;

    private String userAgent;
}

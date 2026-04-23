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
    
    @Override
    public String toString() {
        return "CheckinSubmitRequest{" +
                "labIdentifier='" + labIdentifier + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", capturedAt='" + capturedAt + '\'' +
                ", userAgent='" + userAgent + '\'' +
                '}';
    }
}

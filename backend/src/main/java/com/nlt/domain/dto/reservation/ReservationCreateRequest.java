package com.nlt.domain.dto.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class ReservationCreateRequest {

    @NotNull
    private Long labId;

    private Integer reservationType;

    private Integer priorityLevel;

    @NotBlank
    private String usagePurpose;

    private String courseOrProjectName;

    private Integer participantCount;

    @NotBlank
    private String contactPhone;

    @Valid
    @NotEmpty
    private List<ReservationSlotItem> slots;

    @Data
    public static class ReservationSlotItem {

        @NotBlank
        private String reservationDate;

        @NotNull
        private Long periodId;
    }
}


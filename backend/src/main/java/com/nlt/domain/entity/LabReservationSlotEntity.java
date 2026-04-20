package com.nlt.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LabReservationSlotEntity {

    private Long id;

    private Long reservationId;

    private Long labId;

    private LocalDate reservationDate;

    private Integer weekday;

    private Long periodId;

    private Integer slotStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


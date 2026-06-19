package com.univus.app.reservation.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomReservationSlotDto {
    private Long roomId;
    private Long reservationId;
    private Long reservedMemberId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean available;
}

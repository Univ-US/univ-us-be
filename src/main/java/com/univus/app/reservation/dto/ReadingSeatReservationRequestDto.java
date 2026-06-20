package com.univus.app.reservation.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReadingSeatReservationRequestDto {
    private Long seatId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

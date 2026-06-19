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
public class ReadingSeatRealtimeEventDto {
    private String action;
    private Long seatId;
    private Long readingRoomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

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
public class ReadingSeatAvailabilityDto {
    private Long seatId;
    private Long readingRoomId;
    private String roomName;
    private String seatNumber;
    private String zoneName;
    private Integer isUsable;
    private String seatStatus;
    private Long reservationId;
    private Long reservedMemberId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

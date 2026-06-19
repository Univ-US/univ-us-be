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
public class ReadingRoomAvailabilityDto {
    private Long readingRoomId;
    private String roomName;
    private String floorName;
    private String description;
    private Integer isActive;
    private LocalDateTime createdAt;
    private Integer totalSeatCount;
    private Integer availableSeatCount;
    private Integer usingSeatCount;
    private Integer disabledSeatCount;
}

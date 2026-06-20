package com.univus.app.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

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
public class RoomAvailabilityDto {
    private Long roomId;
    private String roomName;
    private String roomType;
    private String floorName;
    private String location;
    private Integer capacity;
    private String description;
    private Integer isActive;
    private LocalDateTime createdAt;
    private List<RoomReservationSlotDto> slots;
}

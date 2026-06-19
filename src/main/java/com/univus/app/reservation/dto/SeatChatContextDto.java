package com.univus.app.reservation.dto;

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
public class SeatChatContextDto {
    private ActiveSeatReservationDto activeReservation;
    private List<SeatChatRoomDto> rooms;
    private Integer totalUnreadCount;
}

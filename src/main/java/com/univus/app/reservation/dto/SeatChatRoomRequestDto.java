package com.univus.app.reservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SeatChatRoomRequestDto {
    private Long targetReservationId;
}

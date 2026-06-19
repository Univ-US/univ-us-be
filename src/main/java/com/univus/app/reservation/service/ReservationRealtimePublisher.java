package com.univus.app.reservation.service;

import com.univus.app.reservation.dto.ReservationDto;

public interface ReservationRealtimePublisher {

    void publishSeatAfterCommit(
            String action,
            ReservationDto.ReadingSeatReservationDto reservation);

    void publishRoomAfterCommit(
            String action,
            ReservationDto.RoomReservationDto reservation);
}

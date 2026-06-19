package com.univus.app.reservation.service;

import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.RoomReservationDto;

public interface ReservationRealtimePublisher {

    void publishSeatAfterCommit(
            String action,
            ReadingSeatReservationDto reservation);

    void publishRoomAfterCommit(
            String action,
            RoomReservationDto reservation);
}

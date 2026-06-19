package com.univus.app.reservation.service;

import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRequestDto;

public interface ReservationCommandService {

    ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReadingSeatReservationRequestDto request);

    void cancelReadingSeatReservation(Long memberId, Long reservationId);

    RoomReservationDto reserveRoom(
            Long memberId,
            RoomReservationRequestDto request);

    void cancelRoomReservation(Long memberId, Long reservationId);

    void checkInRoom(Long memberId, Long reservationId);

    void checkInReadingSeat(Long memberId, Long reservationId);

    ReadingSeatReservationDto extendReadingSeatReservation(Long memberId, Long reservationId);
}

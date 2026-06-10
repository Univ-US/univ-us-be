package com.univus.app.reservation.service;

import com.univus.app.reservation.dto.ReservationDto;

public interface ReservationCommandService {

    ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request);

    void cancelReadingSeatReservation(Long memberId, Long reservationId);

    ReservationDto.RoomReservationDto reserveRoom(
            Long memberId,
            ReservationDto.RoomReservationRequestDto request);

    void cancelRoomReservation(Long memberId, Long reservationId);
}

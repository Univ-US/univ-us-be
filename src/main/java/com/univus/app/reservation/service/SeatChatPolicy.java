package com.univus.app.reservation.service;

import com.univus.app.reservation.dto.ActiveSeatReservationDto;
import com.univus.app.reservation.dto.SeatChatMessageRequestDto;
import com.univus.app.reservation.dto.SeatChatRoomDto;
import com.univus.app.reservation.dto.SeatChatRoomRequestDto;

public interface SeatChatPolicy {

    void requireMember(Long memberId);

    void validateRoomRequest(SeatChatRoomRequestDto request);

    void validateMessageRequest(SeatChatMessageRequestDto request);

    ActiveSeatReservationDto requireActiveReservation(
            ActiveSeatReservationDto reservation);

    ActiveSeatReservationDto requireTargetReservation(
            ActiveSeatReservationDto reservation,
            String message);

    void requireDifferentSeat(
            Long memberId,
            ActiveSeatReservationDto activeReservation,
            ActiveSeatReservationDto targetReservation);

    SeatChatRoomDto requireParticipantRoom(
            Long roomId,
            SeatChatRoomDto room);

    void requireLockAcquired(boolean locked);
}

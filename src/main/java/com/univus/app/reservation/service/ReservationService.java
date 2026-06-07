package com.univus.app.reservation.service;

import java.time.LocalDateTime;
import java.util.List;

import com.univus.app.reservation.dto.ReservationDto;

public interface ReservationService {

    List<ReservationDto.ReadingRoomAvailabilityDto> getReadingRoomAvailability(
            LocalDateTime startTime,
            LocalDateTime endTime);

    List<ReservationDto.ReadingSeatAvailabilityDto> getReadingSeatAvailability(
            Long readingRoomId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request);

    List<ReservationDto.ReadingSeatReservationDto> getMyReadingSeatReservations(Long memberId);

    void cancelReadingSeatReservation(Long memberId, Long reservationId);
}

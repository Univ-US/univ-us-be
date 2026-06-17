package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.univus.app.reservation.dto.ReservationDto;

public interface ReservationService {

    ReservationDto.ReservationDateOptionsResponseDto getReservationDateOptions(int days);

    List<ReservationDto.ReadingRoomAvailabilityDto> getReadingRoomAvailability(
            Long memberId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    List<ReservationDto.ReadingSeatAvailabilityDto> getReadingSeatAvailability(
            Long memberId,
            Long readingRoomId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request);

    List<ReservationDto.ReadingSeatReservationDto> getMyReadingSeatReservations(Long memberId);

    void cancelReadingSeatReservation(Long memberId, Long reservationId);

    void checkInReadingSeat(Long memberId, Long reservationId);

    ReservationDto.ReadingSeatReservationDto extendReadingSeatReservation(Long memberId, Long reservationId);

    List<ReservationDto.RoomAvailabilityDto> getRoomAvailability(Long memberId, LocalDate date);

    List<ReservationDto.RoomReservationDto> getMyRoomReservations(Long memberId);

    ReservationDto.RoomReservationDto reserveRoom(
            Long memberId,
            ReservationDto.RoomReservationRequestDto request);

    void cancelRoomReservation(Long memberId, Long reservationId);

    void cancelAllPendingReservations(Long memberId);
}

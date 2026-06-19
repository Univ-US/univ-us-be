package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.reservation.dto.ReadingRoomAvailabilityDto;
import com.univus.app.reservation.dto.ReadingSeatAvailabilityDto;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.ReservationDateOptionsResponseDto;
import com.univus.app.reservation.dto.ReservationPenaltyHistoryDto;
import com.univus.app.reservation.dto.ReservationPenaltyPledgeRequestDto;
import com.univus.app.reservation.dto.ReservationPenaltyStatusDto;
import com.univus.app.reservation.dto.RoomAvailabilityDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRequestDto;

public interface ReservationService {

    ReservationDateOptionsResponseDto getReservationDateOptions(int days);

    ReservationPenaltyStatusDto getReservationPenaltyStatus(Long memberId);

    PaginateUtilRestApiRes<ReservationPenaltyHistoryDto> getReservationPenaltyHistory(
            Long memberId,
            Integer page,
            Integer size);

    ReservationPenaltyStatusDto pledgeReservationPenalty(
            Long memberId,
            ReservationPenaltyPledgeRequestDto request);

    List<ReadingRoomAvailabilityDto> getReadingRoomAvailability(
            Long memberId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    List<ReadingSeatAvailabilityDto> getReadingSeatAvailability(
            Long memberId,
            Long readingRoomId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReadingSeatReservationRequestDto request);

    List<ReadingSeatReservationDto> getMyReadingSeatReservations(Long memberId);

    void cancelReadingSeatReservation(Long memberId, Long reservationId);

    void checkInReadingSeat(Long memberId, Long reservationId);

    ReadingSeatReservationDto extendReadingSeatReservation(Long memberId, Long reservationId);

    List<RoomAvailabilityDto> getRoomAvailability(Long memberId, LocalDate date);

    List<RoomReservationDto> getMyRoomReservations(Long memberId);

    RoomReservationDto reserveRoom(
            Long memberId,
            RoomReservationRequestDto request);

    void cancelRoomReservation(Long memberId, Long reservationId);

    void checkInRoom(Long memberId, Long reservationId);

    void cancelAllPendingReservations(Long memberId);
}

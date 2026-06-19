package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.univus.app.reservation.dto.ReservationDto;

public interface ReservationPolicy {

    void requireMember(Long memberId);

    void requireReadingRoom(Long readingRoomId);

    void requireReservationId(Long reservationId);

    void requireReservationDate(LocalDate date);

    ReservationDto.ReadingSeatReservationDto requireSeatReservation(
            ReservationDto.ReadingSeatReservationDto reservation,
            String message);

    ReservationDto.RoomReservationDto requireRoomReservation(
            ReservationDto.RoomReservationDto reservation,
            String message);

    void validateSeatReservationRequest(
            ReservationDto.ReadingSeatReservationRequestDto request);

    void validateRoomReservationRequest(
            ReservationDto.RoomReservationRequestDto request);

    void validateSeatAvailabilityRange(
            LocalDateTime startTime,
            LocalDateTime endTime);

    void validatePenaltyPledge(
            ReservationDto.ReservationPenaltyPledgeRequestDto request,
            int activePenaltyCount);

    void requireCancelableStatus(String status);

    void validateSeatCheckIn(
            ReservationDto.ReadingSeatReservationDto reservation);

    void validateRoomCheckIn(
            ReservationDto.RoomReservationDto reservation);

    void validateRoomCancellation(
            ReservationDto.RoomReservationDto reservation);

    void requirePenaltyAvailable(int activePenaltyCount);

    void requireUsableSeat(int usableSeatCount);

    void requireNoMemberSeatOverlap(int overlapCount);

    void requireNoSeatOverlap(int overlapCount);

    void requireUsableRoom(int usableRoomCount);

    void requireNoRoomOverlap(int overlapCount);

    void requireUpdated(int updated, String message);

    ReservationDto.ReadingSeatReservationDto requireExtendableSeat(
            ReservationDto.ReadingSeatReservationDto reservation);

    LocalDateTime calculateExtendedEndTime(
            ReservationDto.ReadingSeatReservationDto reservation);

    void requireExtensionSlotAvailable(int overlapCount);

    ReservationDto.ReservationPenaltyStatusDto buildPenaltyStatus(
            int activePenaltyCount);

    void requireRoomReservationWindowOpen(LocalDateTime startTime);
}

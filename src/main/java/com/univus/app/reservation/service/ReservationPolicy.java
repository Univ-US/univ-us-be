package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.ReservationPenaltyPledgeRequestDto;
import com.univus.app.reservation.dto.ReservationPenaltyStatusDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRequestDto;

public interface ReservationPolicy {

    void requireMember(Long memberId);

    void requireReadingRoom(Long readingRoomId);

    void requireReservationId(Long reservationId);

    void requireReservationDate(LocalDate date);

    ReadingSeatReservationDto requireSeatReservation(
            ReadingSeatReservationDto reservation,
            String message);

    RoomReservationDto requireRoomReservation(
            RoomReservationDto reservation,
            String message);

    void validateSeatReservationRequest(
            ReadingSeatReservationRequestDto request);

    void validateRoomReservationRequest(
            RoomReservationRequestDto request);

    void validateSeatAvailabilityRange(
            LocalDateTime startTime,
            LocalDateTime endTime);

    void validatePenaltyPledge(
            ReservationPenaltyPledgeRequestDto request,
            int activePenaltyCount);

    void requireCancelableStatus(String status);

    void validateSeatCheckIn(
            ReadingSeatReservationDto reservation);

    void validateRoomCheckIn(
            RoomReservationDto reservation);

    void validateRoomCancellation(
            RoomReservationDto reservation);

    void requirePenaltyAvailable(int activePenaltyCount);

    void requireUsableSeat(int usableSeatCount);

    void requireNoMemberSeatOverlap(int overlapCount);

    void requireNoSeatOverlap(int overlapCount);

    void requireUsableRoom(int usableRoomCount);

    void requireNoRoomOverlap(int overlapCount);

    void requireUpdated(int updated, String message);

    ReadingSeatReservationDto requireExtendableSeat(
            ReadingSeatReservationDto reservation);

    LocalDateTime calculateExtendedEndTime(
            ReadingSeatReservationDto reservation);

    void requireExtensionSlotAvailable(int overlapCount);

    ReservationPenaltyStatusDto buildPenaltyStatus(
            int activePenaltyCount);

    void requireRoomReservationWindowOpen(LocalDateTime startTime);
}

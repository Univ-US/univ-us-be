package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.univus.app.reservation.dto.ReservationDto;

@Component
public class RoomSlotFactory {

    private static final int DAILY_SLOT_COUNT =
            (24 - ReservationPolicy.OPEN_TIME.getHour())
                    / ReservationPolicy.SLOT_HOURS;

    public List<ReservationDto.RoomReservationSlotDto> create(
            Long roomId,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        return IntStream.range(0, DAILY_SLOT_COUNT)
                .mapToObj(index -> createSlot(
                        roomId,
                        date,
                        index,
                        serverNow,
                        reservations))
                .toList();
    }

    public ReservationDto.RoomAvailabilityDto attachSlots(
            ReservationDto.RoomAvailabilityDto room,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        room.setSlots(create(
                room.getRoomId(),
                date,
                serverNow,
                reservations));
        return room;
    }

    private ReservationDto.RoomReservationSlotDto createSlot(
            Long roomId,
            LocalDate date,
            int slotIndex,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        LocalDateTime slotStart = date.atTime(
                ReservationPolicy.OPEN_TIME.plusHours(
                        (long) slotIndex * ReservationPolicy.SLOT_HOURS));
        LocalDateTime slotEnd =
                slotStart.plusHours(ReservationPolicy.SLOT_HOURS);
        Optional<ReservationDto.RoomReservationSlotDto> overlapping =
                findOverlapping(roomId, slotStart, slotEnd, reservations);

        return ReservationDto.RoomReservationSlotDto.builder()
                .roomId(roomId)
                .reservationId(overlapping
                        .map(ReservationDto.RoomReservationSlotDto::getReservationId)
                        .orElse(null))
                .reservedMemberId(overlapping
                        .map(ReservationDto.RoomReservationSlotDto::getReservedMemberId)
                        .orElse(null))
                .startTime(slotStart)
                .endTime(slotEnd)
                .status(overlapping
                        .map(ReservationDto.RoomReservationSlotDto::getStatus)
                        .orElse(null))
                .available(overlapping.isEmpty()
                        && serverNow.isBefore(
                                slotStart.plusMinutes(
                                        ReservationPolicy.CHECK_IN_WINDOW_MINUTES)))
                .build();
    }

    private Optional<ReservationDto.RoomReservationSlotDto> findOverlapping(
            Long roomId,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        return reservations.stream()
                .filter(reservation ->
                        roomId.equals(reservation.getRoomId())
                                && reservation.getStartTime().isBefore(slotEnd)
                                && reservation.getEndTime().isAfter(slotStart))
                .findFirst();
    }
}

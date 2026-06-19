package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.univus.app.reservation.dto.ReservationDto;

@Service
public class RoomSlotFactoryImpl implements RoomSlotFactory {

    private static final int DAILY_SLOT_COUNT =
            (24 - ReservationConstants.OPEN_TIME.getHour())
                    / ReservationConstants.SLOT_HOURS;

    @Override
    public List<ReservationDto.RoomReservationSlotDto> create(
            Long roomId,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        List<ReservationDto.RoomReservationSlotDto> slots =
                new ArrayList<>();
        for (int index = 0; index < DAILY_SLOT_COUNT; index++) {
            ReservationDto.RoomReservationSlotDto slot = createSlot(
                    roomId,
                    date,
                    index,
                    serverNow,
                    reservations);
            slots.add(slot);
        }
        return slots;
    }

    @Override
    public ReservationDto.RoomAvailabilityDto attachSlots(
            ReservationDto.RoomAvailabilityDto room,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        List<ReservationDto.RoomReservationSlotDto> slots = create(
                room.getRoomId(),
                date,
                serverNow,
                reservations);
        room.setSlots(slots);
        return room;
    }

    private ReservationDto.RoomReservationSlotDto createSlot(
            Long roomId,
            LocalDate date,
            int slotIndex,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        LocalDateTime slotStart = date.atTime(
                ReservationConstants.OPEN_TIME.plusHours(
                        (long) slotIndex * ReservationConstants.SLOT_HOURS));
        LocalDateTime slotEnd =
                slotStart.plusHours(ReservationConstants.SLOT_HOURS);
        ReservationDto.RoomReservationSlotDto overlapping =
                findOverlapping(roomId, slotStart, slotEnd, reservations);

        Long reservationId = null;
        Long reservedMemberId = null;
        String status = null;
        if (overlapping != null) {
            reservationId = overlapping.getReservationId();
            reservedMemberId = overlapping.getReservedMemberId();
            status = overlapping.getStatus();
        }

        boolean available =
                overlapping == null
                        && serverNow.isBefore(
                                slotStart.plusMinutes(
                                        ReservationConstants
                                                .CHECK_IN_WINDOW_MINUTES));

        return ReservationDto.RoomReservationSlotDto.builder()
                .roomId(roomId)
                .reservationId(reservationId)
                .reservedMemberId(reservedMemberId)
                .startTime(slotStart)
                .endTime(slotEnd)
                .status(status)
                .available(available)
                .build();
    }

    private ReservationDto.RoomReservationSlotDto findOverlapping(
            Long roomId,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        for (ReservationDto.RoomReservationSlotDto reservation : reservations) {
            boolean sameRoom = roomId.equals(reservation.getRoomId());
            boolean startsBeforeSlotEnd =
                    reservation.getStartTime().isBefore(slotEnd);
            boolean endsAfterSlotStart =
                    reservation.getEndTime().isAfter(slotStart);

            if (sameRoom && startsBeforeSlotEnd && endsAfterSlotStart) {
                return reservation;
            }
        }
        return null;
    }
}

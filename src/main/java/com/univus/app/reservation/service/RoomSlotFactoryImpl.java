package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.univus.app.reservation.dto.RoomAvailabilityDto;
import com.univus.app.reservation.dto.RoomReservationSlotDto;

@Service
public class RoomSlotFactoryImpl implements RoomSlotFactory {

    @Override
    public List<RoomReservationSlotDto> create(
            Long roomId,
            LocalDate date,
            LocalDateTime serverNow,
            List<RoomReservationSlotDto> reservations) {
        List<RoomReservationSlotDto> slots =
                new ArrayList<>();
        for (int index = 0;
                index < ReservationConstants.DAILY_ROOM_SLOT_COUNT;
                index++) {
            RoomReservationSlotDto slot = createSlot(
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
    public RoomAvailabilityDto attachSlots(
            RoomAvailabilityDto room,
            LocalDate date,
            LocalDateTime serverNow,
            List<RoomReservationSlotDto> reservations) {
        List<RoomReservationSlotDto> slots = create(
                room.getRoomId(),
                date,
                serverNow,
                reservations);
        room.setSlots(slots);
        return room;
    }

    private RoomReservationSlotDto createSlot(
            Long roomId,
            LocalDate date,
            int slotIndex,
            LocalDateTime serverNow,
            List<RoomReservationSlotDto> reservations) {
        LocalDateTime slotStart = date.atTime(
                ReservationConstants.OPEN_TIME.plusHours(
                        (long) slotIndex * ReservationConstants.SLOT_HOURS));
        LocalDateTime slotEnd =
                slotStart.plusHours(ReservationConstants.SLOT_HOURS);
        RoomReservationSlotDto overlapping =
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

        return RoomReservationSlotDto.builder()
                .roomId(roomId)
                .reservationId(reservationId)
                .reservedMemberId(reservedMemberId)
                .startTime(slotStart)
                .endTime(slotEnd)
                .status(status)
                .available(available)
                .build();
    }

    private RoomReservationSlotDto findOverlapping(
            Long roomId,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<RoomReservationSlotDto> reservations) {
        for (RoomReservationSlotDto reservation : reservations) {
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

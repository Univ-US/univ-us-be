package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.univus.app.reservation.dto.RoomAvailabilityDto;
import com.univus.app.reservation.dto.RoomReservationSlotDto;

public interface RoomSlotFactory {

    List<RoomReservationSlotDto> create(
            Long roomId,
            LocalDate date,
            LocalDateTime serverNow,
            List<RoomReservationSlotDto> reservations);

    RoomAvailabilityDto attachSlots(
            RoomAvailabilityDto room,
            LocalDate date,
            LocalDateTime serverNow,
            List<RoomReservationSlotDto> reservations);
}

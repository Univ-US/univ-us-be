package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.univus.app.reservation.dto.ReservationDto;

public interface RoomSlotFactory {

    List<ReservationDto.RoomReservationSlotDto> create(
            Long roomId,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations);

    ReservationDto.RoomAvailabilityDto attachSlots(
            ReservationDto.RoomAvailabilityDto room,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations);
}

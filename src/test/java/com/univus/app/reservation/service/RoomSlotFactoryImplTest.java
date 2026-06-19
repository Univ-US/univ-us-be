package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.reservation.dto.ReservationDto;

class RoomSlotFactoryImplTest {

    private final RoomSlotFactory roomSlotFactory =
            new RoomSlotFactoryImpl();

    @Test
    @DisplayName("회의실 운영 시간은 기존과 동일하게 두 시간 단위 8개 슬롯으로 만든다")
    void createBuildsEightSlotsAndKeepsReservationData() {
        LocalDate date = LocalDate.of(2026, 6, 20);
        LocalDateTime reservedStart = date.atTime(10, 0);
        ReservationDto.RoomReservationSlotDto reservation =
                ReservationDto.RoomReservationSlotDto.builder()
                        .reservationId(101L)
                        .reservedMemberId(11L)
                        .roomId(21L)
                        .startTime(reservedStart)
                        .endTime(reservedStart.plusHours(2))
                        .status("RESERVED")
                        .build();

        List<ReservationDto.RoomReservationSlotDto> slots =
                roomSlotFactory.create(
                        21L,
                        date,
                        date.atTime(7, 0),
                        List.of(reservation));

        assertEquals(8, slots.size());
        assertEquals(date.atTime(8, 0), slots.get(0).getStartTime());
        assertTrue(slots.get(0).getAvailable());
        assertEquals(101L, slots.get(1).getReservationId());
        assertEquals(11L, slots.get(1).getReservedMemberId());
        assertFalse(slots.get(1).getAvailable());
        assertEquals(date.plusDays(1).atStartOfDay(), slots.get(7).getEndTime());
    }
}

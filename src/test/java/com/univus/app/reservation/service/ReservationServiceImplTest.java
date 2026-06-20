package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReservationPenaltyHistoryDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

class ReservationServiceImplTest {

    private final ReservationMapper reservationMapper = mock(ReservationMapper.class);
    private final ReservationCommandService reservationCommandService =
            mock(ReservationCommandService.class);
    private final ReservationPolicy reservationPolicy =
            new ReservationPolicyImpl();
    private final ReservationLockExecutor lockExecutor =
            mock(ReservationLockExecutor.class);
    private final RoomSlotFactory roomSlotFactory = mock(RoomSlotFactory.class);
    private final ReservationServiceImpl reservationService =
            new ReservationServiceImpl(
                    reservationMapper,
                    reservationCommandService,
                    reservationPolicy,
                    lockExecutor,
                    roomSlotFactory);

    @Test
    @DisplayName("패널티 이력은 발생 최신순 페이지와 전체 개수를 반환한다")
    void getReservationPenaltyHistoryReturnsPagedResponse() {
        Long memberId = 21L;
        ReservationPenaltyHistoryDto history =
                ReservationPenaltyHistoryDto.builder()
                        .penaltyId(31L)
                        .penaltyType("NO_SHOW")
                        .reason("회의실 노쇼")
                        .status("PLEDGED")
                        .createdAt(LocalDateTime.of(2026, 6, 18, 10, 20))
                        .resolvedAt(LocalDateTime.of(2026, 6, 19, 9, 0))
                        .build();
        when(reservationMapper.selectReservationPenaltyHistory(memberId, 5, 5))
                .thenReturn(List.of(history));
        when(reservationMapper.countReservationPenaltyHistory(memberId))
                .thenReturn(7);

        PaginateUtilRestApiRes<ReservationPenaltyHistoryDto> result =
                reservationService.getReservationPenaltyHistory(memberId, 1, 5);

        assertEquals(List.of(history), result.getContent());
        assertEquals(1, result.getPage());
        assertEquals(7, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        verify(reservationMapper).selectReservationPenaltyHistory(memberId, 5, 5);
    }

    @Test
    @DisplayName("좌석 예약 이력은 서버 페이지와 전체 개수를 반환한다")
    void getReadingSeatReservationHistoryReturnsPagedResponse() {
        Long memberId = 41L;
        ReadingSeatReservationDto reservation =
                ReadingSeatReservationDto.builder()
                        .reservationId(51L)
                        .memberId(memberId)
                        .status("COMPLETED")
                        .build();
        when(reservationMapper.selectMyReadingSeatReservationHistory(
                memberId,
                9,
                9)).thenReturn(List.of(reservation));
        when(reservationMapper.countMyReadingSeatReservations(memberId))
                .thenReturn(12);

        PaginateUtilRestApiRes<ReadingSeatReservationDto> result =
                reservationService.getMyReadingSeatReservationHistory(
                        memberId,
                        1,
                        9);

        assertEquals(List.of(reservation), result.getContent());
        assertEquals(1, result.getPage());
        assertEquals(12, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    @DisplayName("회의실 예약 이력은 서버 페이지와 전체 개수를 반환한다")
    void getRoomReservationHistoryReturnsPagedResponse() {
        Long memberId = 61L;
        RoomReservationDto reservation =
                RoomReservationDto.builder()
                        .reservationId(71L)
                        .memberId(memberId)
                        .status("CANCELLED")
                        .build();
        when(reservationMapper.selectMyRoomReservationHistory(
                memberId,
                0,
                9)).thenReturn(List.of(reservation));
        when(reservationMapper.countMyRoomReservations(memberId))
                .thenReturn(1);

        PaginateUtilRestApiRes<RoomReservationDto> result =
                reservationService.getMyRoomReservationHistory(
                        memberId,
                        0,
                        9);

        assertEquals(List.of(reservation), result.getContent());
        assertEquals(0, result.getPage());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }
}

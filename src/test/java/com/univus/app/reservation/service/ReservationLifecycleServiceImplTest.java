package com.univus.app.reservation.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

class ReservationLifecycleServiceImplTest {

    private final ReservationMapper reservationMapper = mock(ReservationMapper.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ReservationRealtimePublisher realtimePublisher =
            new ReservationRealtimePublisherImpl(messagingTemplate);
    private final ReservationLifecycleService reservationLifecycleService =
            new ReservationLifecycleServiceImpl(
                    reservationMapper,
                    realtimePublisher);

    @Test
    @DisplayName("회의실 만료 예약은 완료되고 노쇼 예약은 패널티와 함께 취소된다")
    void processRoomCompletionAndNoShow() {
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 18, 10, 0);
        LocalDateTime endTime = startTime.plusHours(2);
        ReservationDto.RoomReservationDto expiredReservation =
                ReservationDto.RoomReservationDto.builder()
                        .reservationId(101L)
                        .memberId(11L)
                        .roomId(21L)
                        .startTime(startTime)
                        .endTime(endTime)
                        .status("USING")
                        .build();
        ReservationDto.RoomReservationDto noShowReservation =
                ReservationDto.RoomReservationDto.builder()
                        .reservationId(102L)
                        .memberId(12L)
                        .roomId(22L)
                        .startTime(startTime)
                        .endTime(endTime)
                        .status("RESERVED")
                        .build();

        when(reservationMapper.selectExpiredReadingSeatReservations()).thenReturn(List.of());
        when(reservationMapper.selectNoShowReadingSeatReservations()).thenReturn(List.of());
        when(reservationMapper.selectExpiredRoomReservations())
                .thenReturn(List.of(expiredReservation));
        when(reservationMapper.completeExpiredRoomReservation(101L)).thenReturn(1);
        when(reservationMapper.selectNoShowRoomReservations())
                .thenReturn(List.of(noShowReservation));
        when(reservationMapper.cancelNoShowRoomReservation(102L)).thenReturn(1);

        reservationLifecycleService.processExpiredAndNoShowReservations();

        verify(reservationMapper).completeExpiredRoomReservation(101L);
        verify(reservationMapper).cancelNoShowRoomReservation(102L);
        verify(reservationMapper).insertReservationPenalty(
                12L,
                "NO_SHOW",
                "회의실 예약 시작 후 20분 이내 입실하지 않아 자동 취소되었습니다.");
        verify(messagingTemplate).convertAndSend(
                eq("/sub/reservations/rooms"),
                (Object) argThat(event ->
                        event instanceof ReservationDto.RoomReservationRealtimeEventDto dto
                                && "COMPLETED".equals(dto.getAction())
                                && Long.valueOf(21L).equals(dto.getRoomId())));
        verify(messagingTemplate).convertAndSend(
                eq("/sub/reservations/rooms"),
                (Object) argThat(event ->
                        event instanceof ReservationDto.RoomReservationRealtimeEventDto dto
                                && "CANCELLED".equals(dto.getAction())
                                && Long.valueOf(22L).equals(dto.getRoomId())));
        verify(messagingTemplate).convertAndSendToUser(
                eq("11"),
                eq("/queue/reservations/rooms"),
                any(ReservationDto.RoomReservationRealtimeEventDto.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq("12"),
                eq("/queue/reservations/rooms"),
                any(ReservationDto.RoomReservationRealtimeEventDto.class));
    }

    @Test
    @DisplayName("좌석 노쇼 상태 변경이 선점되지 않으면 패널티를 중복 생성하지 않는다")
    void skipSeatNoShowPenaltyWhenConditionalUpdateMisses() {
        ReservationDto.ReadingSeatReservationDto noShowReservation =
                ReservationDto.ReadingSeatReservationDto.builder()
                        .reservationId(201L)
                        .memberId(31L)
                        .seatId(41L)
                        .readingRoomId(51L)
                        .status("RESERVED")
                        .build();

        when(reservationMapper.selectExpiredReadingSeatReservations()).thenReturn(List.of());
        when(reservationMapper.selectNoShowReadingSeatReservations())
                .thenReturn(List.of(noShowReservation));
        when(reservationMapper.cancelNoShowReadingSeatReservation(201L)).thenReturn(0);
        when(reservationMapper.selectExpiredRoomReservations()).thenReturn(List.of());
        when(reservationMapper.selectNoShowRoomReservations()).thenReturn(List.of());

        reservationLifecycleService.processExpiredAndNoShowReservations();

        verify(reservationMapper).cancelNoShowReadingSeatReservation(201L);
        verify(reservationMapper, never()).insertReservationPenalty(
                eq(31L),
                eq("NO_SHOW"),
                any(String.class));
        verify(messagingTemplate, never()).convertAndSendToUser(
                eq("31"),
                eq("/queue/reservations/seats"),
                any());
    }
}

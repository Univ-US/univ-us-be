package com.univus.app.reservation.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.univus.app.reservation.dto.ReadingSeatRealtimeEventDto;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;

class ReservationRealtimePublisherTest {

    private final SimpMessagingTemplate messagingTemplate =
            mock(SimpMessagingTemplate.class);
    private final ReservationRealtimePublisher publisher =
            new ReservationRealtimePublisherImpl(messagingTemplate);

    @Test
    @DisplayName("좌석 이벤트는 공개 현황 토픽과 예약자 개인 큐에 함께 발행한다")
    void publishSeatEventToPublicAndPrivateDestinations() {
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 19, 10, 0);
        ReadingSeatReservationDto reservation =
                ReadingSeatReservationDto.builder()
                        .memberId(11L)
                        .seatId(21L)
                        .readingRoomId(31L)
                        .startTime(startTime)
                        .endTime(startTime.plusHours(2))
                        .build();

        publisher.publishSeatAfterCommit("RESERVED", reservation);

        verify(messagingTemplate).convertAndSend(
                eq("/sub/reservations/seats"),
                (Object) argThat(event ->
                        event instanceof ReadingSeatRealtimeEventDto dto
                                && "RESERVED".equals(dto.getAction())
                                && Long.valueOf(21L).equals(dto.getSeatId())));
        verify(messagingTemplate).convertAndSendToUser(
                eq("11"),
                eq("/queue/reservations/seats"),
                argThat(event ->
                        event instanceof ReadingSeatRealtimeEventDto dto
                                && Long.valueOf(31L).equals(dto.getReadingRoomId())));
    }
}

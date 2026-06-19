package com.univus.app.reservation.service;

import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.univus.app.reservation.dto.ReservationDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationRealtimePublisher {

    private static final String SEAT_REALTIME_TOPIC = "/sub/reservations/seats";
    private static final String ROOM_REALTIME_TOPIC = "/sub/reservations/rooms";
    private static final String USER_SEAT_REALTIME_QUEUE =
            "/queue/reservations/seats";
    private static final String USER_ROOM_REALTIME_QUEUE =
            "/queue/reservations/rooms";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishSeatAfterCommit(
            String action,
            ReservationDto.ReadingSeatReservationDto reservation) {
        Optional.ofNullable(reservation)
                .ifPresent(value -> afterCommit(() -> publishSeat(action, value)));
    }

    public void publishRoomAfterCommit(
            String action,
            ReservationDto.RoomReservationDto reservation) {
        Optional.ofNullable(reservation)
                .ifPresent(value -> afterCommit(() -> publishRoom(action, value)));
    }

    private void publishSeat(
            String action,
            ReservationDto.ReadingSeatReservationDto reservation) {
        ReservationDto.ReadingSeatRealtimeEventDto event =
                ReservationDto.ReadingSeatRealtimeEventDto.builder()
                        .action(action)
                        .seatId(reservation.getSeatId())
                        .readingRoomId(reservation.getReadingRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();

        messagingTemplate.convertAndSend(SEAT_REALTIME_TOPIC, event);
        messagingTemplate.convertAndSendToUser(
                reservation.getMemberId().toString(),
                USER_SEAT_REALTIME_QUEUE,
                event);
    }

    private void publishRoom(
            String action,
            ReservationDto.RoomReservationDto reservation) {
        ReservationDto.RoomReservationRealtimeEventDto event =
                ReservationDto.RoomReservationRealtimeEventDto.builder()
                        .action(action)
                        .roomId(reservation.getRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();

        messagingTemplate.convertAndSend(ROOM_REALTIME_TOPIC, event);
        messagingTemplate.convertAndSendToUser(
                reservation.getMemberId().toString(),
                USER_ROOM_REALTIME_QUEUE,
                event);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
    }
}

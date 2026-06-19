package com.univus.app.reservation.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.univus.app.reservation.dto.ReadingSeatRealtimeEventDto;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRealtimeEventDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationRealtimePublisherImpl implements ReservationRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishSeatAfterCommit(
            String action,
            ReadingSeatReservationDto reservation) {
        if (reservation == null) {
            return;
        }
        afterCommit(() -> publishSeat(action, reservation));
    }

    @Override
    public void publishRoomAfterCommit(
            String action,
            RoomReservationDto reservation) {
        if (reservation == null) {
            return;
        }
        afterCommit(() -> publishRoom(action, reservation));
    }

    private void publishSeat(
            String action,
            ReadingSeatReservationDto reservation) {
        ReadingSeatRealtimeEventDto event =
                ReadingSeatRealtimeEventDto.builder()
                        .action(action)
                        .seatId(reservation.getSeatId())
                        .readingRoomId(reservation.getReadingRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();

        messagingTemplate.convertAndSend(
                ReservationConstants.SEAT_REALTIME_TOPIC,
                event);
        messagingTemplate.convertAndSendToUser(
                reservation.getMemberId().toString(),
                ReservationConstants.USER_SEAT_REALTIME_QUEUE,
                event);
    }

    private void publishRoom(
            String action,
            RoomReservationDto reservation) {
        RoomReservationRealtimeEventDto event =
                RoomReservationRealtimeEventDto.builder()
                        .action(action)
                        .roomId(reservation.getRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();

        messagingTemplate.convertAndSend(
                ReservationConstants.ROOM_REALTIME_TOPIC,
                event);
        messagingTemplate.convertAndSendToUser(
                reservation.getMemberId().toString(),
                ReservationConstants.USER_ROOM_REALTIME_QUEUE,
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

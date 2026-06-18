package com.univus.app.reservation.service;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ReservationScheduler {

    private static final String SEAT_REALTIME_TOPIC = "/sub/reservations/seats";
    private static final String ROOM_REALTIME_TOPIC = "/sub/reservations/rooms";
    private static final String NO_SHOW_PENALTY_TYPE = "NO_SHOW";
    private static final String NO_SHOW_PENALTY_REASON = "독서실 좌석 입실 가능 시간 내 입실하지 않아 자동 취소되었습니다.";
    private static final String ROOM_NO_SHOW_PENALTY_REASON =
            "회의실 예약 시작 후 20분 이내 입실하지 않아 자동 취소되었습니다.";

    private final ReservationMapper reservationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(cron = "0 * * * * *") // 매 1분마다 실행
    @Transactional
    public void processExpiredAndNoShowReservations() {
        // 1. 만료된 이용 예약 (종료 시간이 지난 USING) -> COMPLETED 처리
        List<ReservationDto.ReadingSeatReservationDto> expiredReservations = 
                reservationMapper.selectExpiredReadingSeatReservations();
                
        for (ReservationDto.ReadingSeatReservationDto reservation : expiredReservations) {
            int updated = reservationMapper.updateReadingSeatReservationStatus(
                    reservation.getReservationId(), "COMPLETED");
            if (updated > 0) {
                log.info("만료된 좌석 예약 자동 완료 처리 (reservationId={})", reservation.getReservationId());
                publishSeatRealtimeEventAfterCommit("COMPLETED", reservation);
            }
        }

        // 2. 노쇼 예약 (입실 가능 시간이 지났는데도 여전히 RESERVED인 경우) -> CANCELLED 처리
        List<ReservationDto.ReadingSeatReservationDto> noShowReservations = 
                reservationMapper.selectNoShowReadingSeatReservations();
                
        for (ReservationDto.ReadingSeatReservationDto reservation : noShowReservations) {
            int updated = reservationMapper.updateReadingSeatReservationStatus(
                    reservation.getReservationId(), "CANCELLED");
            if (updated > 0) {
                reservationMapper.insertReservationPenalty(
                        reservation.getMemberId(),
                        NO_SHOW_PENALTY_TYPE,
                        NO_SHOW_PENALTY_REASON);
                log.info("노쇼 좌석 예약 자동 취소 처리 (reservationId={})", reservation.getReservationId());
                publishSeatRealtimeEventAfterCommit("CANCELLED", reservation);
            }
        }

        // 3. 이용 시간이 끝난 회의실 예약 -> COMPLETED 처리
        List<ReservationDto.RoomReservationDto> expiredRoomReservations =
                reservationMapper.selectExpiredRoomReservations();

        for (ReservationDto.RoomReservationDto reservation : expiredRoomReservations) {
            int updated = reservationMapper.completeExpiredRoomReservation(
                    reservation.getReservationId());
            if (updated > 0) {
                log.info("만료된 회의실 예약 자동 완료 처리 (reservationId={})", reservation.getReservationId());
                publishRoomRealtimeEventAfterCommit("COMPLETED", reservation);
            }
        }

        // 4. 시작 후 20분 동안 입실하지 않은 회의실 예약 -> CANCELLED 및 패널티 처리
        List<ReservationDto.RoomReservationDto> noShowRoomReservations =
                reservationMapper.selectNoShowRoomReservations();

        for (ReservationDto.RoomReservationDto reservation : noShowRoomReservations) {
            int updated = reservationMapper.cancelNoShowRoomReservation(
                    reservation.getReservationId());
            if (updated > 0) {
                reservationMapper.insertReservationPenalty(
                        reservation.getMemberId(),
                        NO_SHOW_PENALTY_TYPE,
                        ROOM_NO_SHOW_PENALTY_REASON);
                log.info("노쇼 회의실 예약 자동 취소 처리 (reservationId={})", reservation.getReservationId());
                publishRoomRealtimeEventAfterCommit("CANCELLED", reservation);
            }
        }
    }

    private void publishSeatRealtimeEventAfterCommit(String action, ReservationDto.ReadingSeatReservationDto reservation) {
        ReservationDto.ReadingSeatRealtimeEventDto event =
                ReservationDto.ReadingSeatRealtimeEventDto.builder()
                        .action(action)
                        .reservationId(reservation.getReservationId())
                        .memberId(reservation.getMemberId())
                        .seatId(reservation.getSeatId())
                        .readingRoomId(reservation.getReadingRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();
        runAfterCommit(() -> messagingTemplate.convertAndSend(SEAT_REALTIME_TOPIC, event));
    }

    private void publishRoomRealtimeEventAfterCommit(
            String action,
            ReservationDto.RoomReservationDto reservation) {
        ReservationDto.RoomReservationRealtimeEventDto event =
                ReservationDto.RoomReservationRealtimeEventDto.builder()
                        .action(action)
                        .reservationId(reservation.getReservationId())
                        .memberId(reservation.getMemberId())
                        .roomId(reservation.getRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();
        runAfterCommit(() -> messagingTemplate.convertAndSend(ROOM_REALTIME_TOPIC, event));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}

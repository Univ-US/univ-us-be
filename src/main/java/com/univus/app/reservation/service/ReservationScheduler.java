package com.univus.app.reservation.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ReservationScheduler {

    private static final String ACTION_COMPLETED = "COMPLETED";
    private static final String ACTION_CANCELLED = "CANCELLED";
    private static final String NO_SHOW_PENALTY_TYPE = "NO_SHOW";
    private static final String SEAT_NO_SHOW_REASON =
            "독서실 좌석 입실 가능 시간 내 입실하지 않아 자동 취소되었습니다.";
    private static final String ROOM_NO_SHOW_REASON =
            "회의실 예약 시작 후 20분 이내 입실하지 않아 자동 취소되었습니다.";

    private final ReservationMapper reservationMapper;
    private final ReservationRealtimePublisher realtimePublisher;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processExpiredAndNoShowReservations() {
        completeExpiredSeatReservations();
        cancelNoShowSeatReservations();
        completeExpiredRoomReservations();
        cancelNoShowRoomReservations();
    }

    private void completeExpiredSeatReservations() {
        for (ReservationDto.ReadingSeatReservationDto reservation
                : reservationMapper.selectExpiredReadingSeatReservations()) {
            int updated =
                    reservationMapper.completeExpiredReadingSeatReservation(
                            reservation.getReservationId());
            if (updated > 0) {
                log.info(
                        "만료된 좌석 예약 자동 완료 처리 (reservationId={})",
                        reservation.getReservationId());
                realtimePublisher.publishSeatAfterCommit(
                        ACTION_COMPLETED,
                        reservation);
            }
        }
    }

    private void cancelNoShowSeatReservations() {
        for (ReservationDto.ReadingSeatReservationDto reservation
                : reservationMapper.selectNoShowReadingSeatReservations()) {
            int updated =
                    reservationMapper.cancelNoShowReadingSeatReservation(
                            reservation.getReservationId());
            if (updated > 0) {
                insertNoShowPenalty(reservation.getMemberId(), SEAT_NO_SHOW_REASON);
                log.info(
                        "노쇼 좌석 예약 자동 취소 처리 (reservationId={})",
                        reservation.getReservationId());
                realtimePublisher.publishSeatAfterCommit(
                        ACTION_CANCELLED,
                        reservation);
            }
        }
    }

    private void completeExpiredRoomReservations() {
        for (ReservationDto.RoomReservationDto reservation
                : reservationMapper.selectExpiredRoomReservations()) {
            int updated =
                    reservationMapper.completeExpiredRoomReservation(
                            reservation.getReservationId());
            if (updated > 0) {
                log.info(
                        "만료된 회의실 예약 자동 완료 처리 (reservationId={})",
                        reservation.getReservationId());
                realtimePublisher.publishRoomAfterCommit(
                        ACTION_COMPLETED,
                        reservation);
            }
        }
    }

    private void cancelNoShowRoomReservations() {
        for (ReservationDto.RoomReservationDto reservation
                : reservationMapper.selectNoShowRoomReservations()) {
            int updated =
                    reservationMapper.cancelNoShowRoomReservation(
                            reservation.getReservationId());
            if (updated > 0) {
                insertNoShowPenalty(reservation.getMemberId(), ROOM_NO_SHOW_REASON);
                log.info(
                        "노쇼 회의실 예약 자동 취소 처리 (reservationId={})",
                        reservation.getReservationId());
                realtimePublisher.publishRoomAfterCommit(
                        ACTION_CANCELLED,
                        reservation);
            }
        }
    }

    private void insertNoShowPenalty(Long memberId, String reason) {
        reservationMapper.insertReservationPenalty(
                memberId,
                NO_SHOW_PENALTY_TYPE,
                reason);
    }
}

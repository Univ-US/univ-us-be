package com.univus.app.reservation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationLifecycleServiceImpl implements ReservationLifecycleService {

    private final ReservationMapper reservationMapper;
    private final ReservationRealtimePublisher realtimePublisher;

    @Transactional
    @Override
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
                        ReservationConstants.ACTION_COMPLETED,
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
                insertNoShowPenalty(
                        reservation.getMemberId(),
                        ReservationConstants.SEAT_NO_SHOW_REASON);
                log.info(
                        "노쇼 좌석 예약 자동 취소 처리 (reservationId={})",
                        reservation.getReservationId());
                realtimePublisher.publishSeatAfterCommit(
                        ReservationConstants.ACTION_CANCELLED,
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
                        ReservationConstants.ACTION_COMPLETED,
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
                insertNoShowPenalty(
                        reservation.getMemberId(),
                        ReservationConstants.ROOM_NO_SHOW_REASON);
                log.info(
                        "노쇼 회의실 예약 자동 취소 처리 (reservationId={})",
                        reservation.getReservationId());
                realtimePublisher.publishRoomAfterCommit(
                        ReservationConstants.ACTION_CANCELLED,
                        reservation);
            }
        }
    }

    private void insertNoShowPenalty(Long memberId, String reason) {
        reservationMapper.insertReservationPenalty(
                memberId,
                ReservationConstants.PENALTY_TYPE_NO_SHOW,
                reason);
    }
}

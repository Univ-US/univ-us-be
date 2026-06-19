package com.univus.app.reservation.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements ReservationCommandService {

    private static final String DEFAULT_STATUS = "RESERVED";
    private static final String ACTION_RESERVED = "RESERVED";
    private static final String ACTION_CANCELLED = "CANCELLED";
    private static final String ACTION_CHECKED_IN = "CHECKED_IN";
    private static final String ACTION_EXTENDED = "EXTENDED";

    private final ReservationMapper reservationMapper;
    private final ReservationPolicy reservationPolicy;
    private final ReservationRealtimePublisher realtimePublisher;

    @Transactional
    @Override
    public ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request) {
        reservationPolicy.requirePenaltyAvailable(
                reservationMapper.countActiveReservationPenalties(memberId));
        reservationPolicy.requireUsableSeat(
                reservationMapper.countUsableReadingSeat(request.getSeatId()));
        reservationPolicy.requireNoMemberSeatOverlap(
                reservationMapper.countOverlappingMemberReadingSeatReservation(
                        memberId,
                        request.getStartTime(),
                        request.getEndTime()));
        reservationPolicy.requireNoSeatOverlap(
                reservationMapper.countOverlappingReadingSeatReservation(
                        request.getSeatId(),
                        request.getStartTime(),
                        request.getEndTime()));

        ReservationDto.ReadingSeatReservationDto reservation =
                ReservationDto.ReadingSeatReservationDto.builder()
                        .memberId(memberId)
                        .seatId(request.getSeatId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .status(DEFAULT_STATUS)
                        .build();

        reservationMapper.insertReadingSeatReservation(reservation);
        ReservationDto.ReadingSeatReservationDto response =
                reservationMapper.selectReadingSeatReservationForMember(
                        reservation.getReservationId(),
                        memberId);
        if (response == null) {
            response = reservation;
        }
        realtimePublisher.publishSeatAfterCommit(ACTION_RESERVED, response);
        return response;
    }

    @Transactional
    @Override
    public void cancelReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        ReservationDto.ReadingSeatReservationDto reservation =
                reservationPolicy.requireSeatReservation(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId),
                        "취소할 수 있는 예약을 찾을 수 없습니다.");
        reservationPolicy.requireUpdated(
                reservationMapper.cancelReadingSeatReservation(
                        reservationId,
                        memberId),
                "취소할 수 있는 예약을 찾을 수 없습니다.");
        realtimePublisher.publishSeatAfterCommit(ACTION_CANCELLED, reservation);
    }

    @Transactional
    @Override
    public ReservationDto.RoomReservationDto reserveRoom(
            Long memberId,
            ReservationDto.RoomReservationRequestDto request) {
        reservationPolicy.requirePenaltyAvailable(
                reservationMapper.countActiveReservationPenalties(memberId));
        reservationPolicy.requireRoomReservationWindowOpen(
                request.getStartTime());
        reservationPolicy.requireUsableRoom(
                reservationMapper.countUsableReservationRoom(
                        request.getRoomId()));
        reservationPolicy.requireNoRoomOverlap(
                reservationMapper.countOverlappingRoomReservation(
                        request.getRoomId(),
                        request.getStartTime(),
                        request.getEndTime()));

        ReservationDto.RoomReservationDto reservation =
                ReservationDto.RoomReservationDto.builder()
                        .memberId(memberId)
                        .roomId(request.getRoomId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .purpose(request.getPurpose())
                        .status(DEFAULT_STATUS)
                        .build();

        reservationMapper.insertRoomReservation(reservation);
        realtimePublisher.publishRoomAfterCommit(ACTION_RESERVED, reservation);
        return reservation;
    }

    @Transactional
    @Override
    public void cancelRoomReservation(
            Long memberId,
            Long reservationId) {
        ReservationDto.RoomReservationDto reservation =
                reservationPolicy.requireRoomReservation(
                        reservationMapper.selectRoomReservationForMember(
                                reservationId,
                                memberId),
                        "취소할 수 있는 공간 예약을 찾을 수 없습니다.");
        reservationPolicy.requireUpdated(
                reservationMapper.cancelRoomReservation(
                        reservationId,
                        memberId),
                "취소할 수 있는 공간 예약을 찾을 수 없습니다.");
        realtimePublisher.publishRoomAfterCommit(ACTION_CANCELLED, reservation);
    }

    @Transactional
    @Override
    public void checkInRoom(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireUpdated(
                reservationMapper.checkInRoomReservation(
                        reservationId,
                        memberId),
                "입실할 수 있는 공간 예약이 아니거나 입실 가능 시간이 지났습니다.");
        realtimePublisher.publishRoomAfterCommit(
                ACTION_CHECKED_IN,
                reservationMapper.selectRoomReservationForMember(
                        reservationId,
                        memberId));
    }

    @Transactional
    @Override
    public void checkInReadingSeat(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireUpdated(
                reservationMapper.checkInReadingSeatReservation(
                        reservationId,
                        memberId),
                "입실할 수 있는 예약이 아니거나 이미 처리되었습니다.");
        realtimePublisher.publishSeatAfterCommit(
                ACTION_CHECKED_IN,
                reservationMapper.selectReadingSeatReservationForMember(
                        reservationId,
                        memberId));
    }

    @Transactional
    @Override
    public ReservationDto.ReadingSeatReservationDto extendReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        ReservationDto.ReadingSeatReservationDto reservation =
                reservationPolicy.requireExtendableSeat(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId));
        LocalDateTime newEndTime =
                reservationPolicy.calculateExtendedEndTime(reservation);
        reservationPolicy.requireExtensionSlotAvailable(
                reservationMapper.countOverlappingReadingSeatReservation(
                        reservation.getSeatId(),
                        reservation.getEndTime(),
                        newEndTime));
        reservationPolicy.requireUpdated(
                reservationMapper.extendReadingSeatReservation(
                        reservationId,
                        memberId,
                        newEndTime),
                "연장 처리에 실패했습니다.");

        ReservationDto.ReadingSeatReservationDto updatedReservation =
                reservationPolicy.requireSeatReservation(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId),
                        "연장된 예약 정보를 찾을 수 없습니다.");
        realtimePublisher.publishSeatAfterCommit(
                ACTION_EXTENDED,
                updatedReservation);
        return updatedReservation;
    }
}

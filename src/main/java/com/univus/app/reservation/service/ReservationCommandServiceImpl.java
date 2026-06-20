package com.univus.app.reservation.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRequestDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements ReservationCommandService {

    private final ReservationMapper reservationMapper;
    private final ReservationPolicy reservationPolicy;
    private final ReservationRealtimePublisher realtimePublisher;

    @Transactional
    @Override
    public ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReadingSeatReservationRequestDto request) {
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

        ReadingSeatReservationDto reservation =
                ReadingSeatReservationDto.builder()
                        .memberId(memberId)
                        .seatId(request.getSeatId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .status(ReservationConstants.STATUS_RESERVED)
                        .build();

        reservationMapper.insertReadingSeatReservation(reservation);
        ReadingSeatReservationDto response =
                reservationMapper.selectReadingSeatReservationForMember(
                        reservation.getReservationId(),
                        memberId);
        if (response == null) {
            response = reservation;
        }
        realtimePublisher.publishSeatAfterCommit(
                ReservationConstants.ACTION_RESERVED,
                response);
        return response;
    }

    @Transactional
    @Override
    public void cancelReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        ReadingSeatReservationDto reservation =
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
        realtimePublisher.publishSeatAfterCommit(
                ReservationConstants.ACTION_CANCELLED,
                reservation);
    }

    @Transactional
    @Override
    public RoomReservationDto reserveRoom(
            Long memberId,
            RoomReservationRequestDto request) {
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

        RoomReservationDto reservation =
                RoomReservationDto.builder()
                        .memberId(memberId)
                        .roomId(request.getRoomId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .purpose(request.getPurpose())
                        .status(ReservationConstants.STATUS_RESERVED)
                        .build();

        reservationMapper.insertRoomReservation(reservation);
        realtimePublisher.publishRoomAfterCommit(
                ReservationConstants.ACTION_RESERVED,
                reservation);
        return reservation;
    }

    @Transactional
    @Override
    public void cancelRoomReservation(
            Long memberId,
            Long reservationId) {
        RoomReservationDto reservation =
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
        realtimePublisher.publishRoomAfterCommit(
                ReservationConstants.ACTION_CANCELLED,
                reservation);
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
                ReservationConstants.ACTION_CHECKED_IN,
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
                ReservationConstants.ACTION_CHECKED_IN,
                reservationMapper.selectReadingSeatReservationForMember(
                        reservationId,
                        memberId));
    }

    @Transactional
    @Override
    public ReadingSeatReservationDto extendReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        ReadingSeatReservationDto reservation =
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

        ReadingSeatReservationDto updatedReservation =
                reservationPolicy.requireSeatReservation(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId),
                        "연장된 예약 정보를 찾을 수 없습니다.");
        realtimePublisher.publishSeatAfterCommit(
                ReservationConstants.ACTION_EXTENDED,
                updatedReservation);
        return updatedReservation;
    }
}

package com.univus.app.reservation.service;

import org.springframework.stereotype.Service;

import com.univus.app.exception.ConflictException;
import com.univus.app.exception.InvalidRequestException;
import com.univus.app.reservation.dto.ActiveSeatReservationDto;
import com.univus.app.reservation.dto.SeatChatMessageRequestDto;
import com.univus.app.reservation.dto.SeatChatRoomDto;
import com.univus.app.reservation.dto.SeatChatRoomRequestDto;

@Service
public class SeatChatPolicyImpl implements SeatChatPolicy {

    @Override
    public void requireMember(Long memberId) {
        if (memberId == null) {
            throw new InvalidRequestException("로그인이 필요합니다.");
        }
    }

    @Override
    public void validateRoomRequest(SeatChatRoomRequestDto request) {
        if (request == null || request.getTargetReservationId() == null) {
            throw new InvalidRequestException(
                    "상대 좌석 예약 ID는 필수입니다.");
        }
    }

    @Override
    public void validateMessageRequest(SeatChatMessageRequestDto request) {
        if (request == null
                || request.getMessageText() == null
                || request.getMessageText().trim().isEmpty()) {
            throw new InvalidRequestException("메시지를 입력해주세요.");
        }
        if (request.getMessageText().trim().length()
                > ReservationConstants.MAX_SEAT_CHAT_MESSAGE_LENGTH) {
            throw new InvalidRequestException(
                    "메시지는 2000자 이내로 입력해주세요.");
        }
    }

    @Override
    public ActiveSeatReservationDto requireActiveReservation(
            ActiveSeatReservationDto reservation) {
        if (reservation == null) {
            throw new ConflictException(
                    "현재 이용 중인 좌석 예약이 있어야 채팅할 수 있습니다.");
        }
        return reservation;
    }

    @Override
    public ActiveSeatReservationDto requireTargetReservation(
            ActiveSeatReservationDto reservation,
            String message) {
        if (reservation == null) {
            throw new ConflictException(message);
        }
        return reservation;
    }

    @Override
    public void requireDifferentSeat(
            Long memberId,
            ActiveSeatReservationDto activeReservation,
            ActiveSeatReservationDto targetReservation) {
        boolean sameReservation =
                activeReservation.getReservationId()
                        .equals(targetReservation.getReservationId());
        boolean sameMember =
                memberId.equals(targetReservation.getMemberId());
        if (sameReservation || sameMember) {
            throw new InvalidRequestException(
                    "본인 좌석에는 메시지를 보낼 수 없습니다.");
        }
    }

    @Override
    public SeatChatRoomDto requireParticipantRoom(
            Long roomId,
            SeatChatRoomDto room) {
        if (roomId == null) {
            throw new InvalidRequestException(
                    "채팅방 ID는 필수입니다.");
        }
        if (room == null) {
            throw new InvalidRequestException(
                    "참여 중인 좌석 채팅방을 찾을 수 없습니다.");
        }
        return room;
    }

    @Override
    public void requireLockAcquired(boolean locked) {
        if (!locked) {
            throw new ConflictException(
                    "채팅방 생성 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
    }
}

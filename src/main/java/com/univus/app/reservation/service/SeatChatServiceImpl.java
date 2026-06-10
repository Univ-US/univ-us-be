package com.univus.app.reservation.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.univus.app.reservation.dto.SeatChatDto;
import com.univus.app.reservation.mapper.SeatChatMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatChatServiceImpl implements SeatChatService {

    private static final String DEFAULT_ROOM_STATUS = "ACTIVE";
    private static final String SEAT_CHAT_TOPIC_PREFIX = "/sub/seat-chats/";
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final SeatChatMapper seatChatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public SeatChatDto.SeatChatContextDto getSeatChatContext(Long memberId) {
        validateMember(memberId);

        SeatChatDto.ActiveSeatReservationDto activeReservation =
                seatChatMapper.selectCurrentActiveSeatReservationForMember(memberId);
        if (activeReservation == null) {
            return SeatChatDto.SeatChatContextDto.builder()
                    .activeReservation(null)
                    .rooms(List.of())
                    .build();
        }

        return SeatChatDto.SeatChatContextDto.builder()
                .activeReservation(activeReservation)
                .rooms(seatChatMapper.selectSeatChatRoomsForReservation(
                        activeReservation.getReservationId()))
                .build();
    }

    @Transactional
    @Override
    public SeatChatDto.SeatChatRoomDto createOrGetSeatChatRoom(
            Long memberId,
            SeatChatDto.SeatChatRoomRequestDto request) {
        validateMember(memberId);
        validateRoomRequest(request);

        SeatChatDto.ActiveSeatReservationDto activeReservation =
                getRequiredActiveReservation(memberId);
        SeatChatDto.ActiveSeatReservationDto targetReservation =
                seatChatMapper.selectCurrentActiveSeatReservationById(
                        request.getTargetReservationId());

        if (targetReservation == null) {
            throw new IllegalStateException("현재 이용 중인 좌석에만 메시지를 보낼 수 있습니다.");
        }
        if (activeReservation.getReservationId().equals(targetReservation.getReservationId())
                || memberId.equals(targetReservation.getMemberId())) {
            throw new IllegalArgumentException("본인 좌석에는 메시지를 보낼 수 없습니다.");
        }

        SeatChatDto.SeatChatRoomDto existingRoom =
                seatChatMapper.selectSeatChatRoomByPair(
                        activeReservation.getReservationId(),
                        targetReservation.getReservationId());
        if (existingRoom != null) {
            return seatChatMapper.selectSeatChatRoomForParticipant(
                    existingRoom.getRoomId(),
                    activeReservation.getReservationId());
        }

        SeatChatDto.SeatChatRoomDto room = SeatChatDto.SeatChatRoomDto.builder()
                .myReservationId(activeReservation.getReservationId())
                .targetReservationId(targetReservation.getReservationId())
                .status(DEFAULT_ROOM_STATUS)
                .build();
        seatChatMapper.insertSeatChatRoom(room);

        return seatChatMapper.selectSeatChatRoomForParticipant(
                room.getRoomId(),
                activeReservation.getReservationId());
    }

    @Override
    public List<SeatChatDto.SeatChatMessageDto> getSeatChatMessages(
            Long memberId,
            Long roomId) {
        SeatChatDto.ActiveSeatReservationDto activeReservation =
                getRequiredActiveReservation(memberId);
        validateParticipant(roomId, activeReservation.getReservationId());

        return seatChatMapper.selectSeatChatMessages(roomId);
    }

    @Transactional
    @Override
    public SeatChatDto.SeatChatMessageDto sendSeatChatMessage(
            Long memberId,
            Long roomId,
            SeatChatDto.SeatChatMessageRequestDto request) {
        validateMessageRequest(request);

        SeatChatDto.ActiveSeatReservationDto activeReservation =
                getRequiredActiveReservation(memberId);
        validateParticipant(roomId, activeReservation.getReservationId());

        SeatChatDto.SeatChatMessageDto message = SeatChatDto.SeatChatMessageDto.builder()
                .roomId(roomId)
                .senderReservationId(activeReservation.getReservationId())
                .senderMemberId(memberId)
                .messageText(request.getMessageText().trim())
                .isRead(0)
                .build();

        seatChatMapper.insertSeatChatMessage(message);
        SeatChatDto.SeatChatMessageDto savedMessage =
                seatChatMapper.selectSeatChatMessage(message.getMessageId());
        SeatChatDto.SeatChatMessageDto response =
                savedMessage == null ? message : savedMessage;

        runAfterCommit(() -> messagingTemplate.convertAndSend(
                SEAT_CHAT_TOPIC_PREFIX + roomId,
                response));

        return response;
    }

    private SeatChatDto.ActiveSeatReservationDto getRequiredActiveReservation(Long memberId) {
        validateMember(memberId);

        SeatChatDto.ActiveSeatReservationDto activeReservation =
                seatChatMapper.selectCurrentActiveSeatReservationForMember(memberId);
        if (activeReservation == null) {
            throw new IllegalStateException("현재 이용 중인 좌석 예약이 있어야 채팅할 수 있습니다.");
        }
        return activeReservation;
    }

    private void validateParticipant(Long roomId, Long reservationId) {
        if (roomId == null) {
            throw new IllegalArgumentException("채팅방 ID는 필수입니다.");
        }

        SeatChatDto.SeatChatRoomDto room =
                seatChatMapper.selectSeatChatRoomForParticipant(roomId, reservationId);
        if (room == null) {
            throw new IllegalArgumentException("참여 중인 좌석 채팅방을 찾을 수 없습니다.");
        }
    }

    private void validateMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }

    private void validateRoomRequest(SeatChatDto.SeatChatRoomRequestDto request) {
        if (request == null || request.getTargetReservationId() == null) {
            throw new IllegalArgumentException("상대 좌석 예약 ID는 필수입니다.");
        }
    }

    private void validateMessageRequest(SeatChatDto.SeatChatMessageRequestDto request) {
        if (request == null || request.getMessageText() == null
                || request.getMessageText().trim().isEmpty()) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }
        if (request.getMessageText().trim().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지는 2000자 이내로 입력해주세요.");
        }
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

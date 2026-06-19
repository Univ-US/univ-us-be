package com.univus.app.reservation.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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

    private final SeatChatMapper seatChatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedissonClient redissonClient;

    @Transactional(readOnly = true)
    @Override
    public SeatChatDto.SeatChatContextDto getSeatChatContext(Long memberId) {
        validateMember(memberId);

        SeatChatDto.ActiveSeatReservationDto activeReservation =
                seatChatMapper.selectCurrentActiveSeatReservationForMember(memberId);
        if (activeReservation == null) {
            return SeatChatDto.SeatChatContextDto.builder()
                    .activeReservation(null)
                    .rooms(List.of())
                    .totalUnreadCount(0)
                    .build();
        }

        List<SeatChatDto.SeatChatRoomDto> rooms =
                seatChatMapper.selectSeatChatRoomsForReservation(
                        activeReservation.getReservationId());
        int totalUnreadCount = 0;
        for (SeatChatDto.SeatChatRoomDto room : rooms) {
            Integer unreadCount = room.getUnreadCount();
            if (unreadCount != null && unreadCount > 0) {
                totalUnreadCount += unreadCount;
            }
        }

        return SeatChatDto.SeatChatContextDto.builder()
                .activeReservation(activeReservation)
                .rooms(rooms)
                .totalUnreadCount(totalUnreadCount)
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

        long minReservationId = Math.min(
                activeReservation.getReservationId(),
                targetReservation.getReservationId());
        long maxReservationId = Math.max(
                activeReservation.getReservationId(),
                targetReservation.getReservationId());
        String lockKey =
                "SEAT_CHAT_ROOM_LOCK:"
                        + minReservationId
                        + "_"
                        + maxReservationId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "채팅방 생성 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            existingRoom = seatChatMapper.selectSeatChatRoomByPair(
                    activeReservation.getReservationId(),
                    targetReservation.getReservationId());
            if (existingRoom != null) {
                return seatChatMapper.selectSeatChatRoomForParticipant(
                    existingRoom.getRoomId(),
                    activeReservation.getReservationId());
            }

            SeatChatDto.SeatChatRoomDto room =
                    SeatChatDto.SeatChatRoomDto.builder()
                            .myReservationId(
                                    activeReservation.getReservationId())
                            .targetReservationId(
                                    targetReservation.getReservationId())
                            .status(ReservationConstants
                                    .CHAT_ROOM_STATUS_ACTIVE)
                            .build();
            seatChatMapper.insertSeatChatRoom(room);

            return seatChatMapper.selectSeatChatRoomForParticipant(
                    room.getRoomId(),
                    activeReservation.getReservationId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "채팅방 생성 중 오류가 발생했습니다.",
                    exception);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<SeatChatDto.SeatChatMessageDto> getSeatChatMessages(
            Long memberId,
            Long roomId) {
        SeatChatDto.ActiveSeatReservationDto activeReservation =
                getRequiredActiveReservation(memberId);
        getRequiredParticipantRoom(roomId, activeReservation.getReservationId());

        return seatChatMapper.selectSeatChatMessages(roomId);
    }

    @Transactional
    @Override
    public void markSeatChatMessagesRead(Long memberId, Long roomId) {
        SeatChatDto.ActiveSeatReservationDto activeReservation =
                getRequiredActiveReservation(memberId);
        getRequiredParticipantRoom(roomId, activeReservation.getReservationId());
        seatChatMapper.markIncomingMessagesRead(
                roomId,
                activeReservation.getReservationId());
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
        SeatChatDto.SeatChatRoomDto room = getRequiredParticipantRoom(
                roomId,
                activeReservation.getReservationId());

        Long targetReservationId = room.getMyReservationId();
        if (room.getMyReservationId()
                .equals(activeReservation.getReservationId())) {
            targetReservationId = room.getTargetReservationId();
        }
        SeatChatDto.ActiveSeatReservationDto targetReservation =
                seatChatMapper.selectCurrentActiveSeatReservationById(
                        targetReservationId);
        if (targetReservation == null) {
            throw new IllegalStateException("상대방이 이미 퇴실하여 메시지를 보낼 수 없습니다.");
        }

        SeatChatDto.SeatChatMessageDto message =
                SeatChatDto.SeatChatMessageDto.builder()
                        .roomId(roomId)
                        .senderReservationId(
                                activeReservation.getReservationId())
                        .senderMemberId(memberId)
                        .messageText(request.getMessageText().trim())
                        .isRead(0)
                        .build();

        seatChatMapper.insertSeatChatMessage(message);
        SeatChatDto.SeatChatMessageDto savedMessage =
                seatChatMapper.selectSeatChatMessage(message.getMessageId());
        SeatChatDto.SeatChatMessageDto response;
        if (savedMessage == null) {
            response = message;
        } else {
            response = savedMessage;
        }
        SeatChatDto.SeatChatNotificationDto notification =
                SeatChatDto.SeatChatNotificationDto.builder()
                        .roomId(roomId)
                        .messageId(response.getMessageId())
                        .senderReservationId(activeReservation.getReservationId())
                        .senderRoomName(activeReservation.getRoomName())
                        .senderSeatNumber(activeReservation.getSeatNumber())
                        .messageText(response.getMessageText())
                        .createdAt(response.getCreatedAt())
                        .build();

        runAfterCommit(() -> {
            messagingTemplate.convertAndSendToUser(
                    memberId.toString(),
                    ReservationConstants.SEAT_CHAT_USER_QUEUE_PREFIX + roomId,
                    response);
            messagingTemplate.convertAndSendToUser(
                    targetReservation.getMemberId().toString(),
                    ReservationConstants.SEAT_CHAT_USER_QUEUE_PREFIX + roomId,
                    response);
            messagingTemplate.convertAndSendToUser(
                    targetReservation.getMemberId().toString(),
                    "/queue/seat-chat-notifications",
                    notification);
        });

        return response;
    }

    private SeatChatDto.ActiveSeatReservationDto getRequiredActiveReservation(
            Long memberId) {
        validateMember(memberId);

        SeatChatDto.ActiveSeatReservationDto activeReservation =
                seatChatMapper.selectCurrentActiveSeatReservationForMember(memberId);
        if (activeReservation == null) {
            throw new IllegalStateException("현재 이용 중인 좌석 예약이 있어야 채팅할 수 있습니다.");
        }
        return activeReservation;
    }

    private SeatChatDto.SeatChatRoomDto getRequiredParticipantRoom(
            Long roomId,
            Long reservationId) {
        if (roomId == null) {
            throw new IllegalArgumentException("채팅방 ID는 필수입니다.");
        }

        SeatChatDto.SeatChatRoomDto room =
                seatChatMapper.selectSeatChatRoomForParticipant(roomId, reservationId);
        if (room == null) {
            throw new IllegalArgumentException("참여 중인 좌석 채팅방을 찾을 수 없습니다.");
        }
        return room;
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
        if (request.getMessageText().trim().length()
                > ReservationConstants.MAX_SEAT_CHAT_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지는 2000자 이내로 입력해주세요.");
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronization synchronization =
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                };
        TransactionSynchronizationManager.registerSynchronization(
                synchronization);
    }
}

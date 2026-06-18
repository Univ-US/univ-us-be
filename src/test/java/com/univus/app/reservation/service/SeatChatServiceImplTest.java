package com.univus.app.reservation.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.univus.app.reservation.dto.SeatChatDto;
import com.univus.app.reservation.mapper.SeatChatMapper;

class SeatChatServiceImplTest {

    private final SeatChatMapper seatChatMapper = mock(SeatChatMapper.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final SeatChatServiceImpl seatChatService =
            new SeatChatServiceImpl(seatChatMapper, messagingTemplate, redissonClient);

    @Test
    @DisplayName("좌석 채팅 메시지는 두 참여자의 개인 큐로만 발행한다")
    void sendSeatChatMessagePublishesToParticipantUserQueues() {
        Long senderMemberId = 10L;
        Long targetMemberId = 20L;
        Long roomId = 30L;
        Long senderReservationId = 100L;
        Long targetReservationId = 200L;

        SeatChatDto.ActiveSeatReservationDto senderReservation =
                SeatChatDto.ActiveSeatReservationDto.builder()
                        .reservationId(senderReservationId)
                        .memberId(senderMemberId)
                        .roomName("중앙도서관")
                        .seatNumber("12")
                        .build();
        SeatChatDto.ActiveSeatReservationDto targetReservation =
                SeatChatDto.ActiveSeatReservationDto.builder()
                        .reservationId(targetReservationId)
                        .memberId(targetMemberId)
                        .build();
        SeatChatDto.SeatChatRoomDto room = SeatChatDto.SeatChatRoomDto.builder()
                .roomId(roomId)
                .myReservationId(senderReservationId)
                .targetReservationId(targetReservationId)
                .build();
        SeatChatDto.SeatChatMessageDto savedMessage =
                SeatChatDto.SeatChatMessageDto.builder()
                        .messageId(300L)
                        .roomId(roomId)
                        .senderReservationId(senderReservationId)
                        .messageText("안녕하세요")
                        .build();

        when(seatChatMapper.selectCurrentActiveSeatReservationForMember(senderMemberId))
                .thenReturn(senderReservation);
        when(seatChatMapper.selectSeatChatRoomForParticipant(roomId, senderReservationId))
                .thenReturn(room);
        when(seatChatMapper.selectCurrentActiveSeatReservationById(targetReservationId))
                .thenReturn(targetReservation);
        doAnswer(invocation -> {
            SeatChatDto.SeatChatMessageDto message = invocation.getArgument(0);
            message.setMessageId(300L);
            return 1;
        }).when(seatChatMapper).insertSeatChatMessage(
                org.mockito.ArgumentMatchers.any(SeatChatDto.SeatChatMessageDto.class));
        when(seatChatMapper.selectSeatChatMessage(300L)).thenReturn(savedMessage);

        SeatChatDto.SeatChatMessageRequestDto request =
                new SeatChatDto.SeatChatMessageRequestDto();
        request.setMessageText("안녕하세요");

        seatChatService.sendSeatChatMessage(senderMemberId, roomId, request);

        String userQueue = "/queue/seat-chats/" + roomId;
        verify(messagingTemplate).convertAndSendToUser(
                senderMemberId.toString(),
                userQueue,
                savedMessage);
        verify(messagingTemplate).convertAndSendToUser(
                targetMemberId.toString(),
                userQueue,
                savedMessage);
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetMemberId.toString()),
                eq("/queue/seat-chat-notifications"),
                argThat(notification ->
                        notification instanceof SeatChatDto.SeatChatNotificationDto dto
                                && roomId.equals(dto.getRoomId())
                                && savedMessage.getMessageId().equals(dto.getMessageId())));
    }
}

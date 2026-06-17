package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuChatDto;
import com.univus.app.lms.mapper.LmsStuChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LmsStuChatServiceImpl implements LmsStuChatService {

    private static final String LMS_CHAT_TOPIC_PREFIX = "/sub/lms-chats/";
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final DateTimeFormatter ISO_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_LABEL =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN);

    private final LmsStuChatMapper lmsStuChatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public List<LmsStuChatDto.ChatRoomResDto> getChatRooms(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        lmsStuChatMapper.insertMissingRooms(lmsPrfId);
        return lmsStuChatMapper.selectChatRooms(lmsPrfId);
    }

    @Override
    @Transactional
    public LmsStuChatDto.ChatThreadResDto getChatThread(Long memberId, Long roomId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        requireStudentRoom(roomId, lmsPrfId);
        lmsStuChatMapper.markIncomingMessagesRead(roomId, lmsPrfId);
        List<LmsStuChatDto.ChatMessageResDto> messages =
                lmsStuChatMapper.selectChatMessages(roomId, lmsPrfId);

        return LmsStuChatDto.ChatThreadResDto.builder()
                .roomId(roomId)
                .dateLabel(firstDateLabel(messages))
                .messages(messages)
                .build();
    }

    @Override
    @Transactional
    public LmsStuChatDto.ChatMessageResDto sendMessage(
            Long memberId,
            Long roomId,
            LmsStuChatDto.SendMessageReqDto request) {
        validateMessageRequest(request);
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        requireStudentRoom(roomId, lmsPrfId);

        LmsStuChatDto.ChatMessageResDto message =
                LmsStuChatDto.ChatMessageResDto.builder()
                        .roomId(roomId)
                        .senderLmsPrfId(lmsPrfId)
                        .text(request.getMessageText().trim())
                        .build();
        lmsStuChatMapper.insertChatMessage(message);
        lmsStuChatMapper.insertUnreadReceipt(message.getId());

        LmsStuChatDto.ChatMessageResDto saved =
                lmsStuChatMapper.selectChatMessage(message.getId(), lmsPrfId);
        LmsStuChatDto.ChatMessageResDto response = saved == null ? message : saved;

        runAfterCommit(() -> messagingTemplate.convertAndSend(
                LMS_CHAT_TOPIC_PREFIX + roomId,
                response));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        Integer count = lmsStuChatMapper.selectUnreadCount(lmsPrfId);
        return count == null ? 0 : count;
    }

    private LmsStuChatDto.ChatRoomResDto requireStudentRoom(Long roomId, Long lmsPrfId) {
        if (roomId == null) {
            throw new IllegalArgumentException("채팅방 ID가 필요합니다.");
        }

        LmsStuChatDto.ChatRoomResDto room =
                lmsStuChatMapper.selectChatRoomForStudent(roomId, lmsPrfId);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.");
        }
        return room;
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long lmsPrfId = lmsStuChatMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private void validateMessageRequest(LmsStuChatDto.SendMessageReqDto request) {
        if (request == null
                || request.getMessageText() == null
                || request.getMessageText().trim().isEmpty()) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }
        if (request.getMessageText().trim().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지는 1000자 이내로 입력해주세요.");
        }
    }

    private String firstDateLabel(List<LmsStuChatDto.ChatMessageResDto> messages) {
        if (messages == null || messages.isEmpty() || messages.get(0).getSentAt() == null) {
            return "";
        }
        return LocalDate.parse(messages.get(0).getSentAt(), ISO_DATE_TIME).format(DATE_LABEL);
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

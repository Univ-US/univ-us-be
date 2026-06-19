package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfChatDto;
import com.univus.app.lms.mapper.LmsProfChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LmsProfChatServiceImpl implements LmsProfChatService {

    private static final String LMS_CHAT_TOPIC_PREFIX = "/sub/lms-chats/";
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final DateTimeFormatter ISO_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_LABEL =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN);

    private final LmsProfChatMapper lmsProfChatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public List<LmsProfChatDto.ChatRoomResDto> getChatRooms(Long memberId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        lmsProfChatMapper.insertMissingRooms(professorLmsPrfId);
        return lmsProfChatMapper.selectChatRooms(professorLmsPrfId);
    }

    @Override
    @Transactional
    public LmsProfChatDto.ChatThreadResDto getChatThread(Long memberId, Long roomId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        requireProfessorRoom(roomId, professorLmsPrfId);
        lmsProfChatMapper.markIncomingMessagesRead(roomId, professorLmsPrfId);
        List<LmsProfChatDto.ChatMessageResDto> messages =
                lmsProfChatMapper.selectChatMessages(roomId, professorLmsPrfId);

        return LmsProfChatDto.ChatThreadResDto.builder()
                .roomId(roomId)
                .dateLabel(firstDateLabel(messages))
                .messages(messages)
                .build();
    }

    @Override
    @Transactional
    public LmsProfChatDto.ChatMessageResDto sendMessage(
            Long memberId,
            Long roomId,
            LmsProfChatDto.SendMessageReqDto request) {
        validateMessageRequest(request);
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        requireProfessorRoom(roomId, professorLmsPrfId);

        LmsProfChatDto.ChatMessageResDto message =
                LmsProfChatDto.ChatMessageResDto.builder()
                        .roomId(roomId)
                        .senderLmsPrfId(professorLmsPrfId)
                        .chtRomMsgContent(request.getMessageText().trim())
                        .build();
        lmsProfChatMapper.insertChatMessage(message);
        lmsProfChatMapper.insertUnreadReceipt(message.getMessageId());

        LmsProfChatDto.ChatMessageResDto saved =
                lmsProfChatMapper.selectChatMessage(message.getMessageId(), professorLmsPrfId);
        LmsProfChatDto.ChatMessageResDto response = saved == null ? message : saved;

        runAfterCommit(() -> messagingTemplate.convertAndSend(
                LMS_CHAT_TOPIC_PREFIX + roomId,
                response));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Long memberId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        Integer count = lmsProfChatMapper.selectUnreadCount(professorLmsPrfId);
        return count == null ? 0 : count;
    }

    private LmsProfChatDto.ChatRoomResDto requireProfessorRoom(Long roomId, Long professorLmsPrfId) {
        if (roomId == null) {
            throw new IllegalArgumentException("채팅방 ID가 필요합니다.");
        }

        LmsProfChatDto.ChatRoomResDto room =
                lmsProfChatMapper.selectChatRoomForProfessor(roomId, professorLmsPrfId);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.");
        }
        return room;
    }

    private Long requireProfessorLmsPrfId(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long lmsPrfId = lmsProfChatMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private void validateMessageRequest(LmsProfChatDto.SendMessageReqDto request) {
        if (request == null
                || request.getMessageText() == null
                || request.getMessageText().trim().isEmpty()) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }
        if (request.getMessageText().trim().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지는 1000자 이내로 입력해주세요.");
        }
    }

    private String firstDateLabel(List<LmsProfChatDto.ChatMessageResDto> messages) {
        if (messages == null || messages.isEmpty() || messages.get(0).getChtRomMsgDate() == null) {
            return "";
        }
        return LocalDateTime.parse(messages.get(0).getChtRomMsgDate(), ISO_DATE_TIME)
                .format(DATE_LABEL);
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

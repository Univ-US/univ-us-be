package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfChatDto;

import java.util.List;

public interface LmsProfChatService {

    List<LmsProfChatDto.ChatRoomResDto> getChatRooms(Long memberId);

    List<LmsProfChatDto.ChatRoomResDto> getStartableRooms(Long memberId);

    LmsProfChatDto.ChatThreadResDto getChatThread(Long memberId, Long roomId);

    LmsProfChatDto.ChatMessageResDto sendMessage(
            Long memberId,
            Long roomId,
            LmsProfChatDto.SendMessageReqDto request);

    int getUnreadCount(Long memberId);

    void deleteChatRoom(Long memberId, Long roomId);
}

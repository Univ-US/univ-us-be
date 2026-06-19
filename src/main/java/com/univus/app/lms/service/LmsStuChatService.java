package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuChatDto;

import java.util.List;

public interface LmsStuChatService {

    List<LmsStuChatDto.ChatRoomResDto> getChatRooms(Long memberId);

    List<LmsStuChatDto.ChatRoomResDto> getStartableRooms(Long memberId);

    LmsStuChatDto.ChatThreadResDto getChatThread(Long memberId, Long roomId);

    LmsStuChatDto.ChatMessageResDto sendMessage(
            Long memberId,
            Long roomId,
            LmsStuChatDto.SendMessageReqDto request);

    int getUnreadCount(Long memberId);
}

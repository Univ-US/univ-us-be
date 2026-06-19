package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuChatDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuChatMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    int insertMissingRooms(@Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuChatDto.ChatRoomResDto> selectChatRooms(@Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuChatDto.ChatRoomResDto> selectStartableRooms(@Param("lmsPrfId") Long lmsPrfId);

    LmsStuChatDto.ChatRoomResDto selectChatRoomForStudent(
            @Param("roomId") Long roomId,
            @Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuChatDto.ChatMessageResDto> selectChatMessages(
            @Param("roomId") Long roomId,
            @Param("lmsPrfId") Long lmsPrfId);

    int markIncomingMessagesRead(
            @Param("roomId") Long roomId,
            @Param("lmsPrfId") Long lmsPrfId);

    int insertChatMessage(LmsStuChatDto.ChatMessageResDto message);

    int insertUnreadReceipt(@Param("messageId") Long messageId);

    LmsStuChatDto.ChatMessageResDto selectChatMessage(
            @Param("messageId") Long messageId,
            @Param("lmsPrfId") Long lmsPrfId);

    Integer selectUnreadCount(@Param("lmsPrfId") Long lmsPrfId);
}

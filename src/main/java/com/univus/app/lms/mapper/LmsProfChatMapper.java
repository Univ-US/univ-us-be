package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfChatDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsProfChatMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    int insertMissingRooms(@Param("professorLmsPrfId") Long professorLmsPrfId);

    List<LmsProfChatDto.ChatRoomResDto> selectChatRooms(
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    List<LmsProfChatDto.ChatRoomResDto> selectStartableRooms(
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    LmsProfChatDto.ChatRoomResDto selectChatRoomForProfessor(
            @Param("roomId") Long roomId,
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    List<LmsProfChatDto.ChatMessageResDto> selectChatMessages(
            @Param("roomId") Long roomId,
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    int markIncomingMessagesRead(
            @Param("roomId") Long roomId,
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    int insertChatMessage(LmsProfChatDto.ChatMessageResDto message);

    int insertUnreadReceipt(@Param("messageId") Long messageId);

    LmsProfChatDto.ChatMessageResDto selectChatMessage(
            @Param("messageId") Long messageId,
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    Integer selectUnreadCount(@Param("professorLmsPrfId") Long professorLmsPrfId);

    int softDeleteRoom(@Param("roomId") Long roomId);
}

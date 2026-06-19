package com.univus.app.inquiry.mapper;

import com.univus.app.inquiry.dto.InquiryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InquiryMapper {

    int insertRoom(InquiryDto.Room room);

    int insertParticipant(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    List<InquiryDto.Room> selectAdminRooms(@Param("univId") Long univId, @Param("memberId") Long memberId);

    List<InquiryDto.Room> selectServiceAdminRooms(@Param("memberId") Long memberId);

    InquiryDto.Room selectRoom(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    InquiryDto.Room selectAdminRoom(
            @Param("roomId") Long roomId,
            @Param("univId") Long univId,
            @Param("memberId") Long memberId);

    List<InquiryDto.Message> selectMessages(@Param("roomId") Long roomId);

    InquiryDto.Message selectMessage(@Param("messageId") Long messageId);

    int insertMessage(InquiryDto.Message message);

    int insertAttachment(InquiryDto.Attachment attachment);

    List<InquiryDto.Attachment> selectAttachmentsByMessageId(@Param("messageId") Long messageId);

    InquiryDto.Attachment selectAttachment(@Param("attachmentId") Long attachmentId);

    int markIncomingMessagesRead(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    int updateRoomStatus(@Param("roomId") Long roomId, @Param("status") String status);
}

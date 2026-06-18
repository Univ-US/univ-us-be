package com.univus.app.reservation.service;

import java.util.List;

import com.univus.app.reservation.dto.SeatChatDto;

public interface SeatChatService {

    SeatChatDto.SeatChatContextDto getSeatChatContext(Long memberId);

    SeatChatDto.SeatChatRoomDto createOrGetSeatChatRoom(
            Long memberId,
            SeatChatDto.SeatChatRoomRequestDto request);

    List<SeatChatDto.SeatChatMessageDto> getSeatChatMessages(
            Long memberId,
            Long roomId);

    void markSeatChatMessagesRead(
            Long memberId,
            Long roomId);

    SeatChatDto.SeatChatMessageDto sendSeatChatMessage(
            Long memberId,
            Long roomId,
            SeatChatDto.SeatChatMessageRequestDto request);
}

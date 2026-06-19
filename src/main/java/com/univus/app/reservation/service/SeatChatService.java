package com.univus.app.reservation.service;

import java.util.List;

import com.univus.app.reservation.dto.SeatChatContextDto;
import com.univus.app.reservation.dto.SeatChatMessageDto;
import com.univus.app.reservation.dto.SeatChatMessageRequestDto;
import com.univus.app.reservation.dto.SeatChatRoomDto;
import com.univus.app.reservation.dto.SeatChatRoomRequestDto;

public interface SeatChatService {

    SeatChatContextDto getSeatChatContext(Long memberId);

    SeatChatRoomDto createOrGetSeatChatRoom(
            Long memberId,
            SeatChatRoomRequestDto request);

    List<SeatChatMessageDto> getSeatChatMessages(
            Long memberId,
            Long roomId);

    void markSeatChatMessagesRead(
            Long memberId,
            Long roomId);

    SeatChatMessageDto sendSeatChatMessage(
            Long memberId,
            Long roomId,
            SeatChatMessageRequestDto request);
}

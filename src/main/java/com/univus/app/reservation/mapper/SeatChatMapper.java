package com.univus.app.reservation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.univus.app.reservation.dto.SeatChatDto;

@Mapper
public interface SeatChatMapper {

    SeatChatDto.ActiveSeatReservationDto selectCurrentActiveSeatReservationForMember(
            @Param("memberId") Long memberId);

    SeatChatDto.ActiveSeatReservationDto selectCurrentActiveSeatReservationById(
            @Param("reservationId") Long reservationId);

    SeatChatDto.SeatChatRoomDto selectSeatChatRoomByPair(
            @Param("reservationId") Long reservationId,
            @Param("targetReservationId") Long targetReservationId);

    int insertSeatChatRoom(SeatChatDto.SeatChatRoomDto room);

    SeatChatDto.SeatChatRoomDto selectSeatChatRoomForParticipant(
            @Param("roomId") Long roomId,
            @Param("reservationId") Long reservationId);

    List<SeatChatDto.SeatChatRoomDto> selectSeatChatRoomsForReservation(
            @Param("reservationId") Long reservationId);

    List<SeatChatDto.SeatChatMessageDto> selectSeatChatMessages(
            @Param("roomId") Long roomId);

    int insertSeatChatMessage(SeatChatDto.SeatChatMessageDto message);

    SeatChatDto.SeatChatMessageDto selectSeatChatMessage(
            @Param("messageId") Long messageId);
}

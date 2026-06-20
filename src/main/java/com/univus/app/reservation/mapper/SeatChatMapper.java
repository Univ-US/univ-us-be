package com.univus.app.reservation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.univus.app.reservation.dto.ActiveSeatReservationDto;
import com.univus.app.reservation.dto.SeatChatMessageDto;
import com.univus.app.reservation.dto.SeatChatRoomDto;

@Mapper
public interface SeatChatMapper {

    ActiveSeatReservationDto selectCurrentActiveSeatReservationForMember(
            @Param("memberId") Long memberId);

    ActiveSeatReservationDto selectCurrentActiveSeatReservationById(
            @Param("reservationId") Long reservationId);

    SeatChatRoomDto selectSeatChatRoomByPair(
            @Param("reservationId") Long reservationId,
            @Param("targetReservationId") Long targetReservationId);

    int insertSeatChatRoom(SeatChatRoomDto room);

    SeatChatRoomDto selectSeatChatRoomForParticipant(
            @Param("roomId") Long roomId,
            @Param("reservationId") Long reservationId);

    List<SeatChatRoomDto> selectSeatChatRoomsForReservation(
            @Param("reservationId") Long reservationId);

    List<SeatChatMessageDto> selectSeatChatMessages(
            @Param("roomId") Long roomId);

    int markIncomingMessagesRead(
            @Param("roomId") Long roomId,
            @Param("reservationId") Long reservationId);

    int insertSeatChatMessage(SeatChatMessageDto message);

    SeatChatMessageDto selectSeatChatMessage(
            @Param("messageId") Long messageId);
}

package com.univus.app.reservation.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.univus.app.reservation.dto.ReadingRoomAvailabilityDto;
import com.univus.app.reservation.dto.ReadingSeatAvailabilityDto;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReservationPenaltyHistoryDto;
import com.univus.app.reservation.dto.RoomAvailabilityDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationSlotDto;

@Mapper
public interface ReservationMapper {

    List<ReadingRoomAvailabilityDto> selectReadingRoomAvailability(
            @Param("memberId") Long memberId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<ReadingSeatAvailabilityDto> selectReadingSeatAvailability(
            @Param("memberId") Long memberId,
            @Param("readingRoomId") Long readingRoomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countOverlappingReadingSeatReservation(
            @Param("seatId") Long seatId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countOverlappingMemberReadingSeatReservation(
            @Param("memberId") Long memberId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countActiveReservationPenalties(@Param("memberId") Long memberId);

    List<ReservationPenaltyHistoryDto> selectReservationPenaltyHistory(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size);

    int countReservationPenaltyHistory(@Param("memberId") Long memberId);

    int insertReservationPenalty(
            @Param("memberId") Long memberId,
            @Param("penaltyType") String penaltyType,
            @Param("reason") String reason);

    int pledgeActiveReservationPenalties(@Param("memberId") Long memberId);

    int countUsableReadingSeat(@Param("seatId") Long seatId);

    int insertReadingSeatReservation(ReadingSeatReservationDto reservation);

    ReadingSeatReservationDto selectReadingSeatReservationForMember(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    List<ReadingSeatReservationDto> selectMyReadingSeatReservations(
            @Param("memberId") Long memberId);

    int cancelReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int cancelAllPendingReadingSeatReservations(@Param("memberId") Long memberId);

    List<RoomAvailabilityDto> selectActiveReservationRooms(@Param("memberId") Long memberId);

    List<RoomReservationSlotDto> selectRoomReservationsBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countUsableReservationRoom(@Param("roomId") Long roomId);

    int countOverlappingRoomReservation(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int insertRoomReservation(RoomReservationDto reservation);

    List<RoomReservationDto> selectMyRoomReservations(
            @Param("memberId") Long memberId);

    RoomReservationDto selectRoomReservationForMember(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int cancelRoomReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int cancelAllPendingRoomReservations(@Param("memberId") Long memberId);

    List<RoomReservationDto> selectExpiredRoomReservations();

    List<RoomReservationDto> selectNoShowRoomReservations();

    int completeExpiredRoomReservation(@Param("reservationId") Long reservationId);

    int cancelNoShowRoomReservation(@Param("reservationId") Long reservationId);

    int checkInRoomReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    List<ReadingSeatReservationDto> selectExpiredReadingSeatReservations();

    List<ReadingSeatReservationDto> selectNoShowReadingSeatReservations();

    int completeExpiredReadingSeatReservation(
            @Param("reservationId") Long reservationId);

    int cancelNoShowReadingSeatReservation(
            @Param("reservationId") Long reservationId);

    int checkInReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int extendReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId,
            @Param("newEndTime") LocalDateTime newEndTime);
}

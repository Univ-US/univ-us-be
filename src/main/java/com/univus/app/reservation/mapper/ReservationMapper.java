package com.univus.app.reservation.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.univus.app.reservation.dto.ReservationDto;

@Mapper
public interface ReservationMapper {

    List<ReservationDto.ReadingRoomAvailabilityDto> selectReadingRoomAvailability(
            @Param("memberId") Long memberId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<ReservationDto.ReadingSeatAvailabilityDto> selectReadingSeatAvailability(
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

    int insertReservationPenalty(
            @Param("memberId") Long memberId,
            @Param("penaltyType") String penaltyType,
            @Param("reason") String reason);

    int pledgeActiveReservationPenalties(@Param("memberId") Long memberId);

    int countUsableReadingSeat(@Param("seatId") Long seatId);

    int insertReadingSeatReservation(ReservationDto.ReadingSeatReservationDto reservation);

    ReservationDto.ReadingSeatReservationDto selectReadingSeatReservationForMember(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    List<ReservationDto.ReadingSeatReservationDto> selectMyReadingSeatReservations(
            @Param("memberId") Long memberId);

    int cancelReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int cancelAllPendingReadingSeatReservations(@Param("memberId") Long memberId);

    List<ReservationDto.RoomAvailabilityDto> selectActiveReservationRooms(@Param("memberId") Long memberId);

    List<ReservationDto.RoomReservationSlotDto> selectRoomReservationsBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countUsableReservationRoom(@Param("roomId") Long roomId);

    int countOverlappingRoomReservation(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int insertRoomReservation(ReservationDto.RoomReservationDto reservation);

    List<ReservationDto.RoomReservationDto> selectMyRoomReservations(
            @Param("memberId") Long memberId);

    ReservationDto.RoomReservationDto selectRoomReservationForMember(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int cancelRoomReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int cancelAllPendingRoomReservations(@Param("memberId") Long memberId);

    List<ReservationDto.RoomReservationDto> selectExpiredRoomReservations();

    List<ReservationDto.RoomReservationDto> selectNoShowRoomReservations();

    int completeExpiredRoomReservation(@Param("reservationId") Long reservationId);

    int cancelNoShowRoomReservation(@Param("reservationId") Long reservationId);

    int checkInRoomReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    List<ReservationDto.ReadingSeatReservationDto> selectExpiredReadingSeatReservations();

    List<ReservationDto.ReadingSeatReservationDto> selectNoShowReadingSeatReservations();

    int updateReadingSeatReservationStatus(
            @Param("reservationId") Long reservationId,
            @Param("status") String status);

    int checkInReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);

    int extendReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId,
            @Param("newEndTime") LocalDateTime newEndTime);
}

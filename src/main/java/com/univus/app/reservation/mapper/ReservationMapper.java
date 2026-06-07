package com.univus.app.reservation.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.univus.app.reservation.dto.ReservationDto;

@Mapper
public interface ReservationMapper {

    List<ReservationDto.ReadingRoomAvailabilityDto> selectReadingRoomAvailability(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<ReservationDto.ReadingSeatAvailabilityDto> selectReadingSeatAvailability(
            @Param("readingRoomId") Long readingRoomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countOverlappingReadingSeatReservation(
            @Param("seatId") Long seatId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    int countUsableReadingSeat(@Param("seatId") Long seatId);

    int insertReadingSeatReservation(ReservationDto.ReadingSeatReservationDto reservation);

    List<ReservationDto.ReadingSeatReservationDto> selectMyReadingSeatReservations(
            @Param("memberId") Long memberId);

    int cancelReadingSeatReservation(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId);
}

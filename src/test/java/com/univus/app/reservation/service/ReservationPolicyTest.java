package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.exception.ConflictException;
import com.univus.app.exception.InvalidRequestException;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;

class ReservationPolicyTest {

    private final ReservationPolicy reservationPolicy =
            new ReservationPolicyImpl();

    @Test
    @DisplayName("좌석 예약 요청의 필수값 검증 메시지를 유지한다")
    void validateSeatReservationRequestRequiresSeatId() {
        ReadingSeatReservationRequestDto request =
                new ReadingSeatReservationRequestDto();

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> reservationPolicy.validateSeatReservationRequest(request));

        assertEquals("좌석 ID는 필수입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("취소 가능한 예약 상태는 RESERVED와 USING뿐이다")
    void requireCancelableStatusRejectsCompletedReservation() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> reservationPolicy.requireCancelableStatus("COMPLETED"));

        assertEquals("이미 취소되었거나 완료된 예약입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("패널티 5회 제한은 기존 409 예외 타입과 메시지를 유지한다")
    void requirePenaltyAvailableKeepsConflictException() {
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> reservationPolicy.requirePenaltyAvailable(5));

        assertEquals(
                ReservationConstants.PENALTY_BLOCK_MESSAGE,
                exception.getMessage());
    }

    @Test
    @DisplayName("회의실 시간 중복은 기존 409 예외 타입과 메시지를 유지한다")
    void requireNoRoomOverlapKeepsConflictException() {
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> reservationPolicy.requireNoRoomOverlap(1));

        assertEquals("이미 예약된 공간입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("사용 중인 좌석은 종료 20분 전부터 두 시간 연장할 수 있다")
    void calculateExtendedEndTime() {
        LocalDateTime endTime = LocalDateTime.now(
                        ReservationConstants.RESERVATION_ZONE)
                .plusMinutes(10);
        ReadingSeatReservationDto reservation =
                ReadingSeatReservationDto.builder()
                        .status("USING")
                        .startTime(endTime.minusHours(2))
                        .endTime(endTime)
                        .build();

        LocalDateTime extendedEndTime =
                reservationPolicy.calculateExtendedEndTime(reservation);

        assertEquals(endTime.plusHours(2), extendedEndTime);
    }
}

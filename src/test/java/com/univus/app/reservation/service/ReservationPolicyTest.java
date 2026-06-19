package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.reservation.dto.ReservationDto;

class ReservationPolicyTest {

    private final ReservationPolicy reservationPolicy = new ReservationPolicy();

    @Test
    @DisplayName("좌석 예약 요청의 필수값 검증 메시지를 유지한다")
    void validateSeatReservationRequestRequiresSeatId() {
        ReservationDto.ReadingSeatReservationRequestDto request =
                new ReservationDto.ReadingSeatReservationRequestDto();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationPolicy.validateSeatReservationRequest(request));

        assertEquals("좌석 ID는 필수입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("취소 가능한 예약 상태는 RESERVED와 USING뿐이다")
    void requireCancelableStatusRejectsCompletedReservation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationPolicy.requireCancelableStatus("COMPLETED"));

        assertEquals("이미 취소되었거나 완료된 예약입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("사용 중인 좌석은 종료 20분 전부터 두 시간 연장할 수 있다")
    void calculateExtendedEndTime() {
        LocalDateTime endTime = LocalDateTime.now(ReservationPolicy.RESERVATION_ZONE)
                .plusMinutes(10);
        ReservationDto.ReadingSeatReservationDto reservation =
                ReservationDto.ReadingSeatReservationDto.builder()
                        .status("USING")
                        .startTime(endTime.minusHours(2))
                        .endTime(endTime)
                        .build();

        LocalDateTime extendedEndTime =
                reservationPolicy.calculateExtendedEndTime(reservation);

        assertEquals(endTime.plusHours(2), extendedEndTime);
    }
}

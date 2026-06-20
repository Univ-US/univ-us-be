package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.exception.ConflictException;
import com.univus.app.exception.InvalidRequestException;
import com.univus.app.reservation.dto.SeatChatMessageRequestDto;

class SeatChatPolicyTest {

    private final SeatChatPolicy seatChatPolicy =
            new SeatChatPolicyImpl();

    @Test
    @DisplayName("빈 메시지는 잘못된 요청 예외로 처리한다")
    void validateMessageRequestRejectsBlankMessage() {
        SeatChatMessageRequestDto request =
                new SeatChatMessageRequestDto();
        request.setMessageText("   ");

        assertThrows(
                InvalidRequestException.class,
                () -> seatChatPolicy.validateMessageRequest(request));
    }

    @Test
    @DisplayName("활성 좌석 예약이 없으면 충돌 예외로 처리한다")
    void requireActiveReservationRejectsMissingReservation() {
        assertThrows(
                ConflictException.class,
                () -> seatChatPolicy.requireActiveReservation(null));
    }
}

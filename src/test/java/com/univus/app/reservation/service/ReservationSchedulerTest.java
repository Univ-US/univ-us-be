package com.univus.app.reservation.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationSchedulerTest {

    private final ReservationLifecycleService reservationLifecycleService =
            mock(ReservationLifecycleService.class);
    private final ReservationScheduler reservationScheduler =
            new ReservationScheduler(reservationLifecycleService);

    @Test
    @DisplayName("예약 스케줄러는 예약 생명주기 처리를 호출한다")
    void processExpiredAndNoShowReservations() {
        reservationScheduler.processExpiredAndNoShowReservations();

        verify(reservationLifecycleService)
                .processExpiredAndNoShowReservations();
    }
}

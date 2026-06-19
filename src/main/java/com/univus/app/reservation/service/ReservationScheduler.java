package com.univus.app.reservation.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationLifecycleService reservationLifecycleService;

    @Scheduled(cron = "0 * * * * *")
    public void processExpiredAndNoShowReservations() {
        reservationLifecycleService.processExpiredAndNoShowReservations();
    }
}

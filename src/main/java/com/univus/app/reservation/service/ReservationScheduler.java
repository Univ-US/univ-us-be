package com.univus.app.reservation.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationLifecycleService reservationLifecycleService;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 * * * * *")
    public void processExpiredAndNoShowReservations() {
        RLock lock = redissonClient.getLock(
                ReservationConstants.SCHEDULER_LOCK_KEY);
        if (!lock.tryLock()) {
            return;
        }

        try {
            reservationLifecycleService.processExpiredAndNoShowReservations();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

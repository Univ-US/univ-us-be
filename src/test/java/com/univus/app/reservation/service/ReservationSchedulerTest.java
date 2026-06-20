package com.univus.app.reservation.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class ReservationSchedulerTest {

    private final ReservationLifecycleService reservationLifecycleService =
            mock(ReservationLifecycleService.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final ReservationScheduler reservationScheduler =
            new ReservationScheduler(
                    reservationLifecycleService,
                    redissonClient);

    @Test
    @DisplayName("분산 락을 획득한 예약 스케줄러만 생명주기 처리를 호출한다")
    void processExpiredAndNoShowReservations() {
        when(redissonClient.getLock(ReservationConstants.SCHEDULER_LOCK_KEY))
                .thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        reservationScheduler.processExpiredAndNoShowReservations();

        verify(reservationLifecycleService)
                .processExpiredAndNoShowReservations();
        verify(lock).unlock();
    }

    @Test
    @DisplayName("다른 서버가 분산 락을 보유하면 생명주기 처리를 건너뛴다")
    void skipWhenAnotherServerOwnsLock() {
        when(redissonClient.getLock(ReservationConstants.SCHEDULER_LOCK_KEY))
                .thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);

        reservationScheduler.processExpiredAndNoShowReservations();

        verify(reservationLifecycleService, never())
                .processExpiredAndNoShowReservations();
    }
}

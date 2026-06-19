package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class ReservationLockExecutorTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final ReservationLockExecutor lockExecutor =
            new ReservationLockExecutor(redissonClient);

    @Test
    @DisplayName("좌석 예약 Lock은 회원과 좌석 순서로 획득하고 역순으로 해제한다")
    void withSeatLocksUsesStableLockOrder() throws InterruptedException {
        RLock memberLock = mock(RLock.class);
        RLock seatLock = mock(RLock.class);
        when(redissonClient.getLock("reservation:reading-seat:member:11"))
                .thenReturn(memberLock);
        when(redissonClient.getLock("reservation:reading-seat:21"))
                .thenReturn(seatLock);
        when(memberLock.tryLock(5, TimeUnit.SECONDS)).thenReturn(true);
        when(seatLock.tryLock(5, TimeUnit.SECONDS)).thenReturn(true);
        when(memberLock.isHeldByCurrentThread()).thenReturn(true);
        when(seatLock.isHeldByCurrentThread()).thenReturn(true);

        String result = lockExecutor.withSeatLocks(11L, 21L, () -> "done");

        assertEquals("done", result);
        InOrder order = inOrder(memberLock, seatLock);
        order.verify(memberLock).tryLock(5, TimeUnit.SECONDS);
        order.verify(seatLock).tryLock(5, TimeUnit.SECONDS);
        order.verify(seatLock).isHeldByCurrentThread();
        order.verify(seatLock).unlock();
        order.verify(memberLock).isHeldByCurrentThread();
        order.verify(memberLock).unlock();
    }

    @Test
    @DisplayName("두 번째 Lock 획득 실패 시 먼저 획득한 Lock을 해제한다")
    void withSeatLocksReleasesAcquiredLockOnFailure()
            throws InterruptedException {
        RLock memberLock = mock(RLock.class);
        RLock seatLock = mock(RLock.class);
        when(redissonClient.getLock("reservation:reading-seat:member:11"))
                .thenReturn(memberLock);
        when(redissonClient.getLock("reservation:reading-seat:21"))
                .thenReturn(seatLock);
        when(memberLock.tryLock(5, TimeUnit.SECONDS)).thenReturn(true);
        when(seatLock.tryLock(5, TimeUnit.SECONDS)).thenReturn(false);
        when(memberLock.isHeldByCurrentThread()).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> lockExecutor.withSeatLocks(11L, 21L, () -> "done"));

        assertEquals(
                "예약 처리 중입니다. 잠시 후 다시 시도해주세요.",
                exception.getMessage());
        verify(memberLock).unlock();
    }
}

package com.univus.app.reservation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationLockExecutor {

    private static final String MEMBER_LOCK_KEY_PREFIX =
            "reservation:reading-seat:member:";
    private static final String SEAT_LOCK_KEY_PREFIX =
            "reservation:reading-seat:";
    private static final String ROOM_LOCK_KEY_PREFIX =
            "reservation:meeting-room:";
    private static final long LOCK_WAIT_SECONDS = 5L;

    private final RedissonClient redissonClient;

    public <T> T withSeatLocks(
            Long memberId,
            Long seatId,
            Supplier<T> operation) {
        return execute(
                List.of(
                        redissonClient.getLock(MEMBER_LOCK_KEY_PREFIX + memberId),
                        redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + seatId)),
                operation);
    }

    public void withSeatLocks(
            Long memberId,
            Long seatId,
            Runnable operation) {
        withSeatLocks(memberId, seatId, asSupplier(operation));
    }

    public <T> T withRoomLock(
            Long roomId,
            Supplier<T> operation) {
        return execute(
                List.of(redissonClient.getLock(ROOM_LOCK_KEY_PREFIX + roomId)),
                operation);
    }

    public void withRoomLock(
            Long roomId,
            Runnable operation) {
        withRoomLock(roomId, asSupplier(operation));
    }

    private <T> T execute(
            List<RLock> locks,
            Supplier<T> operation) {
        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            for (RLock lock : locks) {
                Assert.state(
                        lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS),
                        "예약 처리 중입니다. 잠시 후 다시 시도해주세요.");
                acquiredLocks.add(lock);
            }
            return operation.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("예약 처리가 중단되었습니다.", exception);
        } finally {
            releaseInReverseOrder(acquiredLocks);
        }
    }

    private void releaseInReverseOrder(List<RLock> locks) {
        for (int index = locks.size() - 1; index >= 0; index--) {
            RLock lock = locks.get(index);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Supplier<Void> asSupplier(Runnable operation) {
        return () -> {
            operation.run();
            return null;
        };
    }
}

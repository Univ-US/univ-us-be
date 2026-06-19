package com.univus.app.reservation.service;

import java.util.function.Supplier;

public interface ReservationLockExecutor {

    <T> T withSeatLocks(Long memberId, Long seatId, Supplier<T> operation);

    void withSeatLocks(Long memberId, Long seatId, Runnable operation);

    <T> T withRoomLock(Long roomId, Supplier<T> operation);

    void withRoomLock(Long roomId, Runnable operation);
}

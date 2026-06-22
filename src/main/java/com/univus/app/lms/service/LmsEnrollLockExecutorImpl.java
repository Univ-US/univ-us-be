package com.univus.app.lms.service;

import com.univus.app.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 수강신청 정원 동시성 제어 — 강좌별 Redis 분산 락(reservation 모듈의 ReservationLockExecutor와 동일 패턴)으로
 * 같은 강좌에 대한 동시 신청을 직렬화한다.
 *
 * <p>DB row 락(LmsStuEnrollMapper#lockLectureForEnroll)보다 먼저, 트랜잭션을 시작하기 전에 잡는다.
 * 신청이 몰릴 때 DB 커넥션이 락 대기로 점유되는 대신, 가벼운 Redis 락에서 대기하게 해 DB 커넥션 풀을 보호한다.
 * 컨트롤러가 호출해야 락 획득 → 트랜잭션 시작 순서가 보장된다(서비스 내부에서 호출하면 self-invocation으로 트랜잭션이
 * 락 획득 전에 이미 시작돼버린다).
 */
@Service
@RequiredArgsConstructor
public class LmsEnrollLockExecutorImpl implements LmsEnrollLockExecutor {

    private static final String LECTURE_LOCK_KEY_PREFIX = "lms:enroll:lecture:";
    private static final long LOCK_WAIT_SECONDS = 3L;

    private final RedissonClient redissonClient;

    @Override
    public <T> T withLectureLocks(List<Long> lecIds, Supplier<T> operation) {
        if (lecIds == null || lecIds.isEmpty()) {
            return operation.get();
        }

        // 오름차순 고정 순서로 잠궈 배치 요청 간 락 순서 역전(데드락)을 방지
        List<Long> sortedDistinctLecIds = lecIds.stream().distinct().sorted().toList();
        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            for (Long lecId : sortedDistinctLecIds) {
                RLock lock = redissonClient.getLock(LECTURE_LOCK_KEY_PREFIX + lecId);
                boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
                if (!locked) {
                    throw new ConflictException("수강신청이 몰리고 있습니다. 잠시 후 다시 시도해주세요.");
                }
                acquiredLocks.add(lock);
            }
            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException("수강신청 처리가 중단되었습니다.", e);
        } finally {
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                RLock lock = acquiredLocks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}

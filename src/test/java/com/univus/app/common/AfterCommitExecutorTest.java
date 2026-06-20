package com.univus.app.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AfterCommitExecutorTest {

    private final AfterCommitExecutor executor = new AfterCommitExecutor();

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("트랜잭션이 없으면 작업을 즉시 실행한다")
    void runImmediatelyWithoutTransaction() {
        AtomicInteger count = new AtomicInteger();

        executor.run(count::incrementAndGet);

        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("트랜잭션이 있으면 커밋 이후 작업을 실행한다")
    void runAfterTransactionCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger count = new AtomicInteger();

        executor.run(count::incrementAndGet);

        assertEquals(0, count.get());

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        assertEquals(1, count.get());
    }
}

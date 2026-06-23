package com.univus.app.lms.service;

import com.univus.app.exception.ConflictException;
import com.univus.app.lms.dto.LmsStuEnrollDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 수강신청 요청을 인메모리 큐에 적재하고 워커 스레드 풀로 비동기 처리한다.
 *
 * <p>컨트롤러는 큐 적재 성공 여부만 즉시 응답하고, 실제 처리(Redis 락 → DB 락 → 신청 로직)는
 * 워커 스레드에서 기존 {@link LmsEnrollLockExecutor}·{@link LmsStuEnrollService}를 그대로 호출한다.
 * 처리 결과는 회원별 WebSocket 개인 큐로 푸시한다.
 *
 * <p>큐는 단일 인스턴스 내 메모리에만 존재하므로, 서버 재시작 시 대기 중인 작업은 유실된다
 * (클라이언트가 재시도해야 함). 큐가 가득 차면 {@link #submit}이 false를 반환해 컨트롤러가 즉시 거절한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsEnrollAsyncExecutorImpl implements LmsEnrollAsyncExecutor {

    private static final int QUEUE_CAPACITY = 500;
    private static final int WORKER_COUNT = 4;
    private static final String RESULT_QUEUE_DESTINATION = "/queue/lms/enroll-result";

    private final LmsStuEnrollService lmsStuEnrollService;
    private final LmsEnrollLockExecutor lmsEnrollLockExecutor;
    private final SimpMessagingTemplate messagingTemplate;

    private final BlockingQueue<EnrollJob> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private ExecutorService workerPool;

    @PostConstruct
    void start() {
        workerPool = Executors.newFixedThreadPool(WORKER_COUNT, runnable -> {
            Thread thread = new Thread(runnable, "lms-enroll-worker");
            thread.setDaemon(true);
            return thread;
        });
        for (int i = 0; i < WORKER_COUNT; i++) {
            workerPool.submit(this::runWorkerLoop);
        }
    }

    @PreDestroy
    void shutdown() {
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
    }

    @Override
    public boolean submit(Long memberId, List<Long> lecIds, String requestId) {
        return queue.offer(new EnrollJob(memberId, lecIds, requestId));
    }

    private void runWorkerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                process(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void process(EnrollJob job) {
        LmsStuEnrollDto.EnrollResultPushDto push;
        try {
            LmsStuEnrollDto.SubmitResultDto result = lmsEnrollLockExecutor.withLectureLocks(
                    job.lecIds(), () -> lmsStuEnrollService.submitEnrollment(job.memberId(), job.lecIds()));
            push = LmsStuEnrollDto.EnrollResultPushDto.builder()
                    .requestId(job.requestId())
                    .success(result.getSuccess())
                    .failed(result.getFailed())
                    .build();
        } catch (ConflictException e) {
            push = failedPush(job, e.getMessage());
        } catch (Exception e) {
            log.warn("수강신청 비동기 처리 실패 memberId={} requestId={}: {}", job.memberId(), job.requestId(), e.getMessage());
            push = failedPush(job, "처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
        messagingTemplate.convertAndSendToUser(job.memberId().toString(), RESULT_QUEUE_DESTINATION, push);
    }

    private LmsStuEnrollDto.EnrollResultPushDto failedPush(EnrollJob job, String reason) {
        return LmsStuEnrollDto.EnrollResultPushDto.builder()
                .requestId(job.requestId())
                .success(List.of())
                .failed(job.lecIds().stream()
                        .map(lecId -> LmsStuEnrollDto.FailedItem.builder().lecId(lecId).reason(reason).build())
                        .toList())
                .build();
    }

    private record EnrollJob(Long memberId, List<Long> lecIds, String requestId) {
    }
}

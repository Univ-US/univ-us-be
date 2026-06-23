package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuEnrollDto;
import com.univus.app.lms.service.LmsEnrollAsyncExecutor;
import com.univus.app.lms.service.LmsStuEnrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lms/student/enroll")
@RequiredArgsConstructor
public class LmsStuEnrollController {

    private final LmsStuEnrollService lmsStuEnrollService;
    private final LmsEnrollAsyncExecutor lmsEnrollAsyncExecutor;

    /** 학기 정보 + 신청 가능 학점 */
    @GetMapping("/summary")
    public ResponseEntity<LmsStuEnrollDto.SummaryResDto> requestGetSummary(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuEnrollService.getSummary(memberId));
    }

    /** 개설 강좌 전체 목록 */
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsStuEnrollDto.LectureRow>> requestGetOpenLectures(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuEnrollService.getOpenLectures(memberId));
    }

    /**
     * 장바구니 일괄 신청 — 큐에 적재만 하고 즉시 접수 응답을 반환한다.
     * 실제 처리 결과(정원/시간표/학점 검증)는 처리 완료 후 WebSocket(/user/queue/lms/enroll-result)으로 푸시된다.
     */
    @PostMapping
    public ResponseEntity<LmsStuEnrollDto.AcceptedResDto> requestSubmitEnrollment(
            Authentication authentication,
            @RequestBody LmsStuEnrollDto.SubmitReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        String requestId = UUID.randomUUID().toString();
        boolean accepted = lmsEnrollAsyncExecutor.submit(memberId, request.getLecIds(), requestId);
        if (!accepted) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "수강신청이 몰리고 있습니다. 잠시 후 다시 시도해주세요.");
        }
        LmsStuEnrollDto.AcceptedResDto body = LmsStuEnrollDto.AcceptedResDto.builder()
                .requestId(requestId)
                .message("수강신청이 접수되었습니다. 처리 결과는 알림으로 전달됩니다.")
                .build();
        return ResponseEntity.accepted().body(body);
    }

    /** 신청 취소 */
    @DeleteMapping("/{lecId}")
    public ResponseEntity<Void> requestCancelEnrollment(Authentication authentication, @PathVariable Long lecId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsStuEnrollService.cancelEnrollment(memberId, lecId);
        return ResponseEntity.noContent().build();
    }
}

package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuEnrollDto;
import com.univus.app.lms.service.LmsEnrollLockExecutor;
import com.univus.app.lms.service.LmsStuEnrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student/enroll")
@RequiredArgsConstructor
public class LmsStuEnrollController {

    private final LmsStuEnrollService lmsStuEnrollService;
    private final LmsEnrollLockExecutor lmsEnrollLockExecutor;

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

    /** 장바구니 일괄 신청 — 강좌별 Redis 락(트랜잭션 시작 전)으로 신청 폭주 시 DB 커넥션 점유를 막은 뒤 처리 */
    @PostMapping
    public ResponseEntity<LmsStuEnrollDto.SubmitResultDto> requestSubmitEnrollment(
            Authentication authentication,
            @RequestBody LmsStuEnrollDto.SubmitReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        LmsStuEnrollDto.SubmitResultDto result = lmsEnrollLockExecutor.withLectureLocks(
                request.getLecIds(),
                () -> lmsStuEnrollService.submitEnrollment(memberId, request.getLecIds()));
        return ResponseEntity.ok(result);
    }

    /** 신청 취소 */
    @DeleteMapping("/{lecId}")
    public ResponseEntity<Void> requestCancelEnrollment(Authentication authentication, @PathVariable Long lecId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsStuEnrollService.cancelEnrollment(memberId, lecId);
        return ResponseEntity.noContent().build();
    }
}

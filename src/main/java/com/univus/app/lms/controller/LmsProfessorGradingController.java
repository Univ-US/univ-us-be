package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsGradingDto;
import com.univus.app.lms.service.LmsProfessorGradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/* PLM-004 / PLM-004-01 교수 채점 현황 Controller */
//TODO: SpringSecurity ROLE 가드(PROF/ADM/SUA) 추가 — 현재 authenticated() + service 소유권 검증
@RestController
@RequestMapping("/api/lms/professor/grading")
@RequiredArgsConstructor
public class LmsProfessorGradingController {

    private final LmsProfessorGradingService lmsProfessorGradingService;

    /** PLM-004 채점 개요 (미채점/채점완료 과제 목록 + 배너 카운트) */
    // GET /api/lms/professor/grading/overview?semesterId=
    @GetMapping("/overview")
    public ResponseEntity<LmsGradingDto.Overview> requestGetOverview(
            Authentication authentication,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorGradingService.getOverview(memberId, semesterId));
    }

    /** PLM-004 사이드바 배지: 전 학기 미채점 합 (어느 화면에서든 표시 — overview 학기별 N+1 합산 대체) */
    // GET /api/lms/professor/grading/ungraded-count
    @GetMapping("/ungraded-count")
    public ResponseEntity<LmsGradingDto.UngradedCount> requestGetUngradedCount(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorGradingService.getTotalUngradedCount(memberId));
    }

    /** PLM-004 과제 채점 상세 (수강생별 제출·점수·피드백, 미제출 포함) */
    // GET /api/lms/professor/grading/assignments/{assignmentId}
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<LmsGradingDto.Detail> requestGetAssignmentDetail(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorGradingService.getAssignmentDetail(memberId, assignmentId));
    }

    /** PLM-004 점수·피드백 저장(upsert) → 갱신된 제출 반환 */
    // PUT /api/lms/professor/grading/assignments/{assignmentId}/submissions/{submissionId}
    @PutMapping("/assignments/{assignmentId}/submissions/{submissionId}")
    public ResponseEntity<LmsGradingDto.Submission> requestSaveEvaluation(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody LmsGradingDto.SaveRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                lmsProfessorGradingService.saveEvaluation(memberId, assignmentId, submissionId, request));
    }

    /** PLM-004-01 제출 파일 다운로드 (인증 필요 — FE는 Authorization 헤더로 fetch → blob) */
    // GET /api/lms/professor/grading/submissions/{submissionId}/file
    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<?> requestDownloadSubmissionFile(
            Authentication authentication,
            @PathVariable Long submissionId) {
        Long memberId = (Long) authentication.getPrincipal();
        return lmsProfessorGradingService.downloadSubmissionFile(memberId, submissionId);
    }
}

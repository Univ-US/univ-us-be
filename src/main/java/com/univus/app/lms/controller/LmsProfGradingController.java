package com.univus.app.lms.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsProfGradingDto;
import com.univus.app.lms.service.LmsProfGradingService;
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
@RestController
@RequestMapping("/api/lms/professor/grading")
@RequiredArgsConstructor
public class LmsProfGradingController {

    private final LmsProfGradingService lmsProfGradingService;

    /** PLM-004 채점 개요 — 배너 카운트(미채점 합 + 과목별). 년도/학기 null이면 전체 */
    // GET /api/lms/professor/grading/overview?year=&termCode=
    @GetMapping("/overview")
    public ResponseEntity<LmsProfGradingDto.OverviewResDto> requestGetOverview(
            Authentication authentication,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "termCode", required = false) String termCode) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfGradingService.getOverview(memberId, year, termCode));
    }

    /** PLM-004 과제 목록 — 서버 페이지네이션(미채점/채점 분리 + 년도/학기 필터). graded=false 미채점/true 채점완료 */
    // GET /api/lms/professor/grading/assignments?graded=&year=&termCode=&page=&size=
    @GetMapping("/assignments")
    public ResponseEntity<PaginateUtilRestApiRes<LmsProfGradingDto.AssignmentResDto>> requestGetAssignments(
            Authentication authentication,
            @RequestParam(value = "graded", required = false, defaultValue = "false") boolean graded,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "termCode", required = false) String termCode,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        int p = page == null ? 0 : page;
        int s = size == null ? 5 : size;
        return ResponseEntity.ok(lmsProfGradingService.getAssignments(memberId, graded, year, termCode, p, s));
    }

    /** PLM-004 사이드바 배지: 전 학기 미채점 합 (어느 화면에서든 표시 — overview 학기별 N+1 합산 대체) */
    // GET /api/lms/professor/grading/ungraded-count
    @GetMapping("/ungraded-count")
    public ResponseEntity<LmsProfGradingDto.UngradedCountResDto> requestGetUngradedCount(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfGradingService.getTotalUngradedCount(memberId));
    }

    /** PLM-004 과제 채점 상세 (수강생별 제출·점수·피드백, 미제출 포함) */
    // GET /api/lms/professor/grading/assignments/{assignmentId}
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<LmsProfGradingDto.DetailResDto> requestGetAssignmentDetail(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfGradingService.getAssignmentDetail(memberId, assignmentId));
    }

    /** PLM-004 점수·피드백 저장(upsert) → 갱신된 제출 반환 */
    // PUT /api/lms/professor/grading/assignments/{assignmentId}/submissions/{submissionId}
    @PutMapping("/assignments/{assignmentId}/submissions/{submissionId}")
    public ResponseEntity<LmsProfGradingDto.SubmissionResDto> requestSaveEvaluation(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody LmsProfGradingDto.SaveReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(
                lmsProfGradingService.saveEvaluation(memberId, assignmentId, submissionId, request));
    }

    /** PLM-004-01 제출 파일 다운로드 (인증 필요 — FE는 Authorization 헤더로 fetch → blob) */
    // GET /api/lms/professor/grading/submissions/{submissionId}/file
    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<?> requestDownloadSubmissionFile(
            Authentication authentication,
            @PathVariable Long submissionId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsProfGradingService.downloadSubmissionFile(memberId, submissionId);
    }
}

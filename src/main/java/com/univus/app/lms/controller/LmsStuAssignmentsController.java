package com.univus.app.lms.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import com.univus.app.lms.service.LmsStuAssignmentsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student/assignments")
@RequiredArgsConstructor
public class LmsStuAssignmentsController {

    private final LmsStuAssignmentsService lmsStuAssignmentsService;

    @GetMapping
    public ResponseEntity<LmsStuAssignmentsDto.AssignmentsResultResDto> requestGetAssignments(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuAssignmentsService.getAssignments(memberId));
    }

    /** 학기별 과제 요약 (카드 헤더 — 상태 필터. 과제는 학기별 페이지 조회) */
    // GET /api/lms/student/assignments/semesters?status=
    @GetMapping("/semesters")
    public ResponseEntity<List<LmsStuAssignmentsDto.AssignmentSemesterSummaryResDto>> requestGetAssignmentSemesters(
            Authentication authentication,
            @RequestParam(value = "status", required = false) String status) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuAssignmentsService.getAssignmentSemesterSummaries(memberId, status));
    }

    /** 한 학기(semId)의 과제 1페이지 — 상태 필터, 서버 페이지네이션. page 0-based, size 기본 10 */
    // GET /api/lms/student/assignments/semesters/{semId}?status=&page=&size=
    @GetMapping("/semesters/{semId}")
    public ResponseEntity<PaginateUtilRestApiRes<LmsStuAssignmentsDto.StudentAssignmentResDto>> requestGetSemesterAssignments(
            Authentication authentication,
            @PathVariable Long semId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        return ResponseEntity.ok(lmsStuAssignmentsService.getSemesterAssignmentsPaged(memberId, semId, status, p, s));
    }

    /** 제출 가능 과제 요약 — 사이드바 배지(전역 미제출 수) + 연도 필터 드롭다운 소스 */
    // GET /api/lms/student/assignments/submittable/summary
    @GetMapping("/submittable/summary")
    public ResponseEntity<LmsStuAssignmentsDto.SubmittableSummaryResDto> requestGetSubmittableSummary(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuAssignmentsService.getSubmittableSummary(memberId));
    }

    /** 제출 가능 과제 1페이지 — 서버 페이지네이션. page 0-based, size 기본 10. focusAssignmentId=딥링크 대상 */
    // GET /api/lms/student/assignments/submittable?year=&term=&page=&size=&focusAssignmentId=
    @GetMapping("/submittable")
    public ResponseEntity<PaginateUtilRestApiRes<LmsStuAssignmentsDto.SubmittableAssignmentResDto>> requestGetSubmittable(
            Authentication authentication,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "term", required = false) String term,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "focusAssignmentId", required = false) Long focusAssignmentId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        return ResponseEntity.ok(lmsStuAssignmentsService.getSubmittable(memberId, year, term, p, s, focusAssignmentId));
    }

    @PostMapping(value = "/{assignmentId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> requestSubmitAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @Valid @ModelAttribute LmsStuAssignmentsDto.SubmitReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsStuAssignmentsService.submitAssignment(memberId, assignmentId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/submissions/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> requestUpdateSubmission(
            Authentication authentication,
            @PathVariable Long submissionId,
            @Valid @ModelAttribute LmsStuAssignmentsDto.UpdateSubmissionReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsStuAssignmentsService.updateSubmission(memberId, submissionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<?> requestDownloadSubmissionFile(
            Authentication authentication,
            @PathVariable Long submissionId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsStuAssignmentsService.downloadSubmissionFile(memberId, submissionId);
    }
}

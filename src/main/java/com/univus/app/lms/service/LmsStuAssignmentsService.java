package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import org.springframework.http.ResponseEntity;

public interface LmsStuAssignmentsService {

    LmsStuAssignmentsDto.AssignmentsResultResDto getAssignments(Long memberId);

    /** 제출 가능 과제 요약 — 전역 미제출 수(배지) + 연도 드롭다운 소스 */
    LmsStuAssignmentsDto.SubmittableSummaryResDto getSubmittableSummary(Long memberId);

    /** 제출 가능 과제 1페이지 (서버 페이지네이션). focusAssignmentId 있으면 그 과제가 속한 페이지 반환 */
    PaginateUtilRestApiRes<LmsStuAssignmentsDto.SubmittableAssignmentResDto> getSubmittable(
            Long memberId, Integer year, String term, int page, int size, Long focusAssignmentId);

    void submitAssignment(Long memberId, Long assignmentId, LmsStuAssignmentsDto.SubmitReqDto request);

    void updateSubmission(Long memberId, Long submissionId, LmsStuAssignmentsDto.UpdateSubmissionReqDto request);

    ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId);
}

package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsProfGradingDto;
import org.springframework.http.ResponseEntity;

/** PLM-004 / PLM-004-01 교수 채점 현황 서비스 */
public interface LmsProfGradingService {

    /** 채점 개요 — 배너 카운트(미채점 합 + 과목별). 년도/학기 null이면 전체 */
    LmsProfGradingDto.OverviewResDto getOverview(Long memberId, Integer year, String termCode);

    /** 과제 목록 1페이지 — 서버 페이지네이션(미채점/채점 분리 + 년도/학기 필터). graded=false 미채점/true 채점완료, page 0-based */
    PaginateUtilRestApiRes<LmsProfGradingDto.AssignmentResDto> getAssignments(
            Long memberId, boolean graded, Integer year, String termCode, int page, int size);

    /** 사이드바 배지용 전 학기 미채점 합 */
    LmsProfGradingDto.UngradedCountResDto getTotalUngradedCount(Long memberId);

    /** 과제 채점 상세 (수강생별 제출·점수·피드백, 미제출 포함) */
    LmsProfGradingDto.DetailResDto getAssignmentDetail(Long memberId, Long assignmentId);

    /** 점수·피드백 저장(upsert) → 갱신된 제출 반환 */
    LmsProfGradingDto.SubmissionResDto saveEvaluation(Long memberId, Long assignmentId, Long submissionId,
                                                      LmsProfGradingDto.SaveReqDto request);

    /** 제출 파일 다운로드 (인증 필요) */
    ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId);
}

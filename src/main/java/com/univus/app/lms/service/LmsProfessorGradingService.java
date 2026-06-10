package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsGradingDto;
import org.springframework.http.ResponseEntity;

/** PLM-004 / PLM-004-01 교수 채점 현황 서비스 */
public interface LmsProfessorGradingService {

    /** 채점 개요 (미채점/채점완료 과제 목록 + 배너 카운트) */
    LmsGradingDto.Overview getOverview(Long memberId, Long semesterId);

    /** 사이드바 배지용 전 학기 미채점 합 */
    LmsGradingDto.UngradedCount getTotalUngradedCount(Long memberId);

    /** 과제 채점 상세 (수강생별 제출·점수·피드백, 미제출 포함) */
    LmsGradingDto.Detail getAssignmentDetail(Long memberId, Long assignmentId);

    /** 점수·피드백 저장(upsert) → 갱신된 제출 반환 */
    LmsGradingDto.Submission saveEvaluation(Long memberId, Long assignmentId, Long submissionId,
                                            LmsGradingDto.SaveRequest request);

    /** 제출 파일 다운로드 (인증 필요) */
    ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId);
}

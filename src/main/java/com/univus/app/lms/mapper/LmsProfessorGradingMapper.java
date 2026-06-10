package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsGradingDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/** PLM-004 / PLM-004-01 교수 채점 현황 매퍼 */
@Mapper
public interface LmsProfessorGradingMapper {

    /** 교수 본인 LMS_PRF_ID (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 학기 (semId 주면 그 학기, null이면 최신 학기) */
    LmsSemesterResponseDto findSemester(@Param("semId") Long semId);

    /** 개요: 교수 담당 강의 과제 + 제출/채점 카운트 (semId optional) */
    List<LmsGradingDto.AssignmentRow> selectOverviewAssignments(
            @Param("professorLmsPrfId") Long professorLmsPrfId, @Param("semId") Long semId);

    /** 사이드바 배지: 교수 전 학기 미채점 합 (overview totalUngraded와 동일 규칙, 학기 필터 없음) */
    int sumUngradedAcrossSemesters(@Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 과제 헤더(+소유권 검증). 본인 강의 아니면 null */
    LmsGradingDto.Detail selectAssignmentHeader(
            @Param("assignmentId") Long assignmentId, @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 상세: 수강생 전체(미제출 포함) + 제출/평가 LEFT JOIN */
    List<LmsGradingDto.Submission> selectAssignmentSubmissions(
            @Param("assignmentId") Long assignmentId, @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 과제의 모든 제출 첨부 (제출별 등록일 DESC — service가 제출별 최신 1건 선택) */
    List<LmsGradingDto.FileRow> selectAssignmentFiles(@Param("assignmentId") Long assignmentId);

    /** 단일 제출 재조회 (저장 후 반환용) */
    LmsGradingDto.Submission selectSubmission(
            @Param("submissionId") Long submissionId, @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 단일 제출 첨부 (등록일 DESC) */
    List<LmsGradingDto.FileRow> selectSubmissionFiles(@Param("submissionId") Long submissionId);

    /** 저장 소유권 검증: 제출이 (해당 과제 & 교수 본인 강의)에 속하는지 */
    int countOwnedSubmission(@Param("submissionId") Long submissionId,
                             @Param("assignmentId") Long assignmentId,
                             @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 다운로드 소유권 검증: 제출이 교수 본인 강의에 속하는지 */
    int countOwnedSubmissionById(@Param("submissionId") Long submissionId,
                                 @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 점수·피드백 upsert (제출당 평가 1건) */
    void upsertEvaluation(@Param("submissionId") Long submissionId,
                          @Param("score") BigDecimal score,
                          @Param("feedback") String feedback);
}

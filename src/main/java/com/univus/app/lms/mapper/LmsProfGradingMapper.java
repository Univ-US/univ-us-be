package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfGradingDto;
import com.univus.app.lms.dto.LmsProfSemesterDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/** PLM-004 / PLM-004-01 교수 채점 현황 매퍼 */
@Mapper
public interface LmsProfGradingMapper {

    /** 교수 본인 LMS_PRF_ID (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 학기 (semId 주면 그 학기, null이면 최신 학기) */
    LmsProfSemesterDto findSemester(@Param("semId") Long semId);

    /** 과제 목록 개수 (서버 페이지네이션 — 미채점/채점 분리 + 년도/학기 필터. ungradedOnly=true면 미채점, false면 채점완료) */
    int countGradingAssignments(@Param("professorLmsPrfId") Long professorLmsPrfId,
                                @Param("year") Integer year,
                                @Param("termCode") String termCode,
                                @Param("ungradedOnly") boolean ungradedOnly);

    /** 과제 목록 1페이지 (미채점/채점 분리 + 년도/학기 필터 + OFFSET/FETCH, 마감일 순) */
    List<LmsProfGradingDto.AssignmentRow> selectGradingAssignmentsPaged(
            @Param("professorLmsPrfId") Long professorLmsPrfId,
            @Param("year") Integer year,
            @Param("termCode") String termCode,
            @Param("ungradedOnly") boolean ungradedOnly,
            @Param("offset") int offset,
            @Param("size") int size);

    /** 미채점 합계 (배너·사이드바 배지 공용 — year/termCode null이면 전 학기, 지정 시 그 범위) */
    int sumUngraded(@Param("professorLmsPrfId") Long professorLmsPrfId,
                    @Param("year") Integer year,
                    @Param("termCode") String termCode);

    /** 과목별 미채점 건수 (배너 byCourse — 선택 필터 범위, 미채점>0 과목만) */
    List<LmsProfGradingDto.CourseCountRow> selectUngradedByCourse(
            @Param("professorLmsPrfId") Long professorLmsPrfId,
            @Param("year") Integer year,
            @Param("termCode") String termCode);

    /** 과제 헤더(+소유권 검증). 본인 강의 아니면 null */
    LmsProfGradingDto.DetailRow selectAssignmentHeader(
            @Param("assignmentId") Long assignmentId, @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 상세: 수강생 전체(미제출 포함) + 제출/평가 LEFT JOIN */
    List<LmsProfGradingDto.SubmissionRow> selectAssignmentSubmissions(
            @Param("assignmentId") Long assignmentId, @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 과제의 모든 제출 첨부 (제출별 등록일 DESC — service가 제출별 최신 1건 선택) */
    List<LmsProfGradingDto.FileRow> selectAssignmentFiles(@Param("assignmentId") Long assignmentId);

    /** 단일 제출 재조회 (저장 후 반환용) */
    LmsProfGradingDto.SubmissionRow selectSubmission(
            @Param("submissionId") Long submissionId, @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 단일 제출 첨부 (등록일 DESC) */
    List<LmsProfGradingDto.FileRow> selectSubmissionFiles(@Param("submissionId") Long submissionId);

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

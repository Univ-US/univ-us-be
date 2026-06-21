package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuAssignmentsMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    List<LmsStuAssignmentsDto.AssignmentFlatRow> selectAssignmentRows(@Param("lmsPrfId") Long lmsPrfId);

    /** 학기별 과제 요약 (상태 필터 — 매칭 과제 있는 학기 + 건수, 최신순) */
    List<LmsStuAssignmentsDto.AssignmentSemesterSummaryRow> selectAssignmentSemesterSummaries(
            @Param("lmsPrfId") Long lmsPrfId, @Param("status") String status);

    /** 한 학기의 과제 개수 (상태 필터 — 페이지네이션 total) */
    long countSemesterAssignments(@Param("lmsPrfId") Long lmsPrfId,
                                  @Param("semId") Long semId,
                                  @Param("status") String status);

    /** 한 학기의 과제 1페이지 (마감 최신순 + 상태 필터 + OFFSET/FETCH) */
    List<LmsStuAssignmentsDto.AssignmentFlatRow> selectSemesterAssignmentsPaged(
            @Param("lmsPrfId") Long lmsPrfId,
            @Param("semId") Long semId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("size") int size);

    /** 제출 가능 과제 개수 (year/term null이면 전역 — 배지/요약·목록 count 공용) */
    long countSubmittable(@Param("lmsPrfId") Long lmsPrfId,
                          @Param("year") Integer year,
                          @Param("term") String term);

    /** 제출 가능 과제 1페이지 (마감 임박순 + OFFSET/FETCH) */
    List<LmsStuAssignmentsDto.SubmittableRow> selectSubmittablePaged(@Param("lmsPrfId") Long lmsPrfId,
                                                                     @Param("year") Integer year,
                                                                     @Param("term") String term,
                                                                     @Param("offset") int offset,
                                                                     @Param("size") int size);

    /** 제출 가능 과제가 존재하는 연도 목록 (연도 필터 드롭다운 소스, 내림차순) */
    List<Integer> selectSubmittableYears(@Param("lmsPrfId") Long lmsPrfId);

    /** 딥링크 대상 과제의 1-based 순번 (목록 정렬 기준 동일). 없으면 null → 호출측 페이지 0 */
    Integer selectSubmittableRank(@Param("lmsPrfId") Long lmsPrfId,
                                  @Param("year") Integer year,
                                  @Param("term") String term,
                                  @Param("focusAssignmentId") Long focusAssignmentId);

    LmsStuAssignmentsDto.SubmitAccessRow selectSubmitAccess(
            @Param("assignmentId") Long assignmentId,
            @Param("lmsPrfId") Long lmsPrfId);

    LmsStuAssignmentsDto.SubmitAccessRow selectSubmissionAccess(
            @Param("submissionId") Long submissionId,
            @Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuAssignmentsDto.FileRow> selectSubmissionFiles(@Param("submissionId") Long submissionId);

    int insertSubmission(LmsStuAssignmentsDto.SubmitParam param);

    int updateSubmissionAsSubmitted(
            @Param("submissionId") Long submissionId,
            @Param("memo") String memo);

    int updateSubmissionMemo(
            @Param("submissionId") Long submissionId,
            @Param("memo") String memo);

    int updateSubmissionAsNotSubmitted(@Param("submissionId") Long submissionId);

    int insertSubmissionAttachment(
            @Param("submissionId") Long submissionId,
            @Param("orgFileName") String orgFileName,
            @Param("trnFileName") String trnFileName,
            @Param("orgUrl") String orgUrl,
            @Param("fileSize") Long fileSize,
            @Param("extType") String extType);

    int invalidateSubmissionAttachments(@Param("submissionId") Long submissionId);
}

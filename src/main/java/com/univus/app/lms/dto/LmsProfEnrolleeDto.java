package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PLM-003 / PLM-003-01 수강생 현황 DTO 묶음 (3분류 네스티드).
 * <p>다중 엔드포인트 컨테이너: 매핑 row(Row/ReportRow/AssignmentScoreRow) + 응답(ListResDto/RowResDto/
 * SummaryResDto/ReportResDto/AssignmentScoreResDto) + 페이지메타(PageInfo).
 * service가 매핑 row → ResDto 변환(출석률·총 과제 수·제출/채점 여부 계산 보강).
 * <p>강의 헤더 {@link LmsProfLectureDto}·학기 {@link LmsProfSemesterDto}는 여러 기능이 공유하므로 top-level 유지.
 */
public final class LmsProfEnrolleeDto {

    private LmsProfEnrolleeDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType, 내부 전용 ===== */

    /** 수강생 목록 1행 (selectStudentRows) — SQL raw 집계 포함 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Row {
        private Long enrollmentId;        // LECTURE_STUDENT_ENROLLMENT.LEC_STD_ENR_ID
        private Long memberId;            // 학생 MEMBER_ID
        private String studentName;       // MEMBER.MEMBER_NAME
        private String studentNo;         // MEMBER.LOGIN_ID (학번)
        private String imageUrl;          // 최신 유효 프로필 이미지 URL (nullable)
        private Integer submittedCount;   // 제출 과제 수
        private Integer averageScore;     // 평균 점수 (채점된 과제 평균, nullable)
        private Integer presentCount;     // 출석(PRS) 횟수 (출석률 계산용)
        private Integer attendanceTotal;  // 전체 출석 기록 수 (출석률 분모)
    }

    /** 상세 리포트 헤더 1행 (selectStudentReportHeader) — SQL raw 집계 포함 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportRow {
        private Long memberId;
        private String studentName;
        private String studentNo;
        private String imageUrl;
        private String lectureName;       // LECTURE_CODE.LEC_COD_NAME
        private Long studentLmsPrfId;     // 과제별 점수 2차 조회용 (내부 전용)
        private Integer attendancePresent;
        private Integer attendanceLate;
        private Integer attendanceAbsent;
        private Integer attendanceTotal;
        private Integer submittedCount;
        private Integer averageScore;
    }

    /** 과제별 점수 1행 (selectAssignmentScores) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentScoreRow {
        private Long assignmentId;        // LECTURE_ASSIGNMENT.LEC_ASN_ID (자기 PK — 의미명 유지)
        private String lecAsnTitle;       // LECTURE_ASSIGNMENT.LEC_ASN_TITLE (앵커 자기 컬럼)
        private Integer asnSbmEvlScore;   // ASSIGNMENT_SUBMISSION_EVALUATION.ASN_SBM_EVL_SCORE (미제출/미채점이면 null)
        private String lecAsnSbmStatus;   // LECTURE_ASSIGNMENT_SUBMISSION.LEC_ASN_SBM_STATUS (NSB/SBM/GRD/RTN, 제출 없으면 null)
    }

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = FE 계약(불변) ===== */

    /** PLM-003 목록 응답: 강의 헤더 + 통계 카드 3개 + 수강생 목록(현재 페이지) + 페이지메타 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResDto {
        private LmsProfLectureDto lecture;        // 강의 헤더(이름·학기) — 공유 DTO
        private SummaryResDto summary;            // 통계 3카드 (검색 전체 기준)
        private List<RowResDto> students;         // 수강생 목록 (현재 페이지)
        private PageInfo pagination;              // 페이지네이션 메타 (검색·필터 적용 후 전체 기준)
    }

    /** PLM-003 수강생 목록 1행 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowResDto {
        private Long enrollmentId;
        private Long memberId;
        private String studentName;
        private String studentNo;
        private String imageUrl;
        private int attendanceRate;       // 출석률(%) — service 계산
        private Integer submittedCount;
        private Integer totalAssignments; // 전체 과제 수 — service 세팅(강의 단위)
        private Integer averageScore;
    }

    /** PLM-003 상단 통계 카드 3개 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResDto {
        private int totalStudents;          // 전체 수강생 (수강 인원)
        private int averageAttendanceRate;  // 평균 출석률(%)
        private int averageSubmissionRate;  // 과제 제출률(%)
    }

    /** PLM-003-01 수강생 상세 리포트 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportResDto {
        private Long memberId;
        private String studentName;
        private String studentNo;
        private String imageUrl;
        private String lectureName;
        // 상단 요약 카드 3개
        private int attendanceRate;            // service 계산
        private Integer submittedCount;
        private Integer totalAssignments;      // service 세팅
        private Integer averageScore;
        // 출결 요약 (출석/지각/결석)
        private Integer attendancePresent;
        private Integer attendanceLate;
        private Integer attendanceAbsent;
        private Integer attendanceTotal;       // 출석률 분모
        // 과제별 점수
        private List<AssignmentScoreResDto> assignmentScores;
    }

    /** PLM-003-01 과제별 점수 1줄 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentScoreResDto {
        private Long assignmentId;
        private String lecAsnTitle;           // LECTURE_ASSIGNMENT.LEC_ASN_TITLE
        private Integer asnSbmEvlScore;       // ASSIGNMENT_SUBMISSION_EVALUATION.ASN_SBM_EVL_SCORE (미제출/미채점이면 null)
        private String lecAsnSbmStatus;       // LECTURE_ASSIGNMENT_SUBMISSION.LEC_ASN_SBM_STATUS (NSB/SBM/GRD/RTN, 제출 없으면 null)
        private String submissionStatusLabel; // 미사용(항상 null, FE 공통코드 매핑) — 파생, 의미명 유지
        private boolean submitted;            // 제출 여부 (NSB 아님) — service
        private boolean scored;               // 채점 여부 (asnSbmEvlScore != null) — service
    }

    /** 페이지네이션 메타 (검색·제출필터 적용 후 전체 기준) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageInfo {
        private int page;          // 현재 페이지 (0-based)
        private int size;          // 페이지 크기
        private int totalElements; // 필터 적용된 전체 건수
        private int totalPages;    // 전체 페이지 수 (수강생 0~size명이어도 최소 1)
    }
}

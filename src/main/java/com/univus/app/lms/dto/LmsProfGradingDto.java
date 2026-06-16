package com.univus.app.lms.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * PLM-004 / PLM-004-01 교수 채점 현황 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12).
 * <p>다중 엔드포인트 컨테이너: 매핑 row(AssignmentRow/CourseCountRow/DetailRow/SubmissionRow/FileRow)
 * + 응답(OverviewResDto/AssignmentResDto/CourseCountResDto/UngradedCountResDto/DetailResDto/SubmissionResDto/SubmissionFileResDto)
 * + 요청(SaveReqDto). service가 매핑 row → ResDto 변환(미채점수·만점·채점여부·파일 보강).
 * <p>MyBatis resultType은 {@code com.univus.app.lms.dto.LmsProfGradingDto$Inner} 형태로 참조.
 */
public final class LmsProfGradingDto {

    private LmsProfGradingDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType, 내부 전용 ===== */

    /** 과제 1행 매핑 (selectGradingAssignmentsPaged) — SQL 7컬럼 1:1 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentRow {
        private Long assignmentId;
        private String courseName;
        private Integer lecSection;    // 분반 (LECTURE.LEC_SECTION)
        private String title;
        private String dueDate;        // "YYYY.MM.DD" (TO_CHAR)
        private int submittedCount;    // 제출 학생 수 (LEC_ASN_SBM_STATUS != 'NSB')
        private int gradedCount;       // 채점완료 수 (평가 점수 not null)
        private int totalStudents;     // 수강생 수 (LEC_STD_ENR_STATUS != 'DRP') — 미채점=총원−채점 계산용
    }

    /** 과목별 미채점 건수 매핑 (selectUngradedByCourse) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseCountRow {
        private String courseName;
        private int count;
    }

    /** 과제 헤더 매핑 (selectAssignmentHeader) — SQL 4컬럼 1:1 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRow {
        private Long assignmentId;
        private String courseName;
        private String title;
        private String dueDate;
    }

    /** 수강생별 제출/채점 1행 매핑 (selectAssignmentSubmissions / selectSubmission) — SQL 8컬럼 1:1 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionRow {
        private Long submissionId;       // 미제출(제출행 없음)이면 null
        private Long memberId;
        private String studentName;
        private String studentNo;        // MEMBER.LOGIN_ID
        private String submittedAt;      // "MM.DD HH:mm" (미제출 null)
        private String submissionStatus; // LEC_ASN_SBM_STATUS (NSB 등)
        private BigDecimal score;        // null = 미채점
        private String feedback;
    }

    /** 제출 첨부 매핑 (selectAssignmentFiles / selectSubmissionFiles) — 내부 전용(응답 미노출) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileRow {
        private Long submissionId;
        private String fileName;    // 원본 파일명
        private Long fileSize;
        private String contentType; // 확장자
        private String trnFileName; // 저장(변환) 파일명
        private String orgUrl;      // 원본 url
    }

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = FE 계약(불변) ===== */

    /** 채점 개요 응답 (GET /grading/overview) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewResDto {
        private int totalUngraded;              // 미채점 수강생 수 합(총원−채점, 선택 필터 범위) — 2026-06-16 정의 통일
        private List<CourseCountResDto> byCourse; // 과목별 미채점 수강생 수 (배너용)
    }

    /** 과목별 미채점 건수 응답 (byCourse) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseCountResDto {
        private String courseName;
        private int count;
    }

    /** 과제 1행 응답 (GET /grading/assignments) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentResDto {
        private Long assignmentId;
        private String courseName;
        private Integer lecSection;    // FE가 "N반" 표기
        private String title;
        private String dueDate;        // "YYYY.MM.DD"
        private int submittedCount;
        private int gradedCount;
        private int ungradedCount;     // 미채점 = 총원 − 채점완료 (채점 안 된 수강생 수, service 계산)
        private Integer maxScore;      // 과제 만점 (현재 100 고정 — service)
    }

    /** 사이드바 배지용 전 학기 미채점 합 응답 (GET /grading/ungraded-count) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UngradedCountResDto {
        private int totalUngraded;
    }

    /** 과제 채점 상세 응답 (GET /grading/assignments/{id}) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResDto {
        private Long assignmentId;
        private String courseName;
        private String title;
        private Integer maxScore;
        private String dueDate;
        private int gradedCount;
        private int ungradedCount;
        private List<SubmissionResDto> submissions;
    }

    /** 수강생별 제출/채점 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionResDto {
        private Long submissionId;       // 미제출이면 null
        private Long memberId;
        private String studentName;
        private String studentNo;
        private String submittedAt;
        private String submissionStatus;
        private BigDecimal score;        // null = 미채점
        private String feedback;         // null→"" (service)
        private boolean graded;          // score != null (service)
        private SubmissionFileResDto file; // 미제출/무첨부면 null (service 보강)
    }

    /** 제출 파일 메타 응답 (FE 단일 file). 첨부 1:N 중 최신 1건만 노출 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionFileResDto {
        private String fileName;    // ASN_SBM_ATT_ORG_FIL_NAME
        private Long fileSize;      // ASN_SBM_ATT_FIL_SIZE (bytes)
        private String fileUrl;     // 인증 다운로드 엔드포인트 경로
        private String contentType; // ASN_SBM_ATT_EXT_TYPE (확장자)
    }

    /* ===== 요청(ReqDto) ===== */

    /** 점수·피드백 저장 요청 (PUT) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveReqDto {
        // null 허용(미채점 되돌림) — Bean Validation은 null을 통과시키므로 점수를 비우면 채점 취소.
        // 만점 100 고정(DEFAULT_MAX_SCORE) · 점수 컬럼 NUMBER(5,2) → 정수 3자리·소수 2자리.
        @DecimalMin(value = "0", message = "점수는 0 이상이어야 합니다.")
        @DecimalMax(value = "100", message = "점수는 100 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "점수는 소수점 둘째 자리까지만 입력할 수 있습니다.")
        private BigDecimal score;
        @Size(max = 200, message = "피드백은 200자 이내여야 합니다.")
        private String feedback;  // null/빈문자 허용, 최대 200자
    }
}

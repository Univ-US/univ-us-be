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
 * PLM-004 / PLM-004-01 교수 채점 현황 DTO 묶음.
 * 응답·요청 DTO + MyBatis 매핑용 row DTO를 한 파일에 중첩으로 모음.
 * (MyBatis resultType은 com.univus.app.lms.dto.LmsGradingDto$Inner 형태로 참조)
 */
public final class LmsGradingDto {

    private LmsGradingDto() {
    }

    /** 채점 개요 응답 (GET /grading/overview) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Overview {
        private int totalUngraded;          // 미채점 제출 건수 합 (선택 필터 범위)
        private List<CourseCount> byCourse; // 과목별 미채점 제출 건수 (배너용)
        // 미채점/채점 과제 목록은 서버 페이지네이션 별도 엔드포인트(GET /grading/assignments)로 분리(2026-06-13)
    }

    /** 과제 1행 (개요 목록). MyBatis가 6필드 채우고 service가 ungradedCount·maxScore 보강 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentRow {
        private Long assignmentId;
        private String courseName;
        private Integer lecSection;    // 분반 (LECTURE.LEC_SECTION) — FE가 "N반" 표기
        private String title;
        private String dueDate;        // "YYYY.MM.DD" (TO_CHAR)
        private int submittedCount;    // 제출 학생 수 (LEC_ASN_SBM_STATUS != 'NSB')
        private int gradedCount;       // 채점완료 수 (평가 점수 not null)
        private int ungradedCount;     // submittedCount - gradedCount (service 계산)
        private Integer maxScore;      // 과제 만점 (현재 100 고정 — 스키마 미보유, 추후 재검토)
    }

    /** 과목별 미채점 건수 (byCourse) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseCount {
        private String courseName;
        private int count;
    }

    /** 사이드바 배지용 전 학기 미채점 합 (GET /grading/ungraded-count) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UngradedCount {
        private int totalUngraded; // overview totalUngraded와 동일 규칙을 교수의 모든 학기에 합산
    }

    /** 과제 채점 상세 응답 (GET /grading/assignments/{id}) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Detail {
        private Long assignmentId;
        private String courseName;
        private String title;
        private Integer maxScore;
        private String dueDate;
        private int gradedCount;
        private int ungradedCount;
        private List<Submission> submissions;
    }

    /** 수강생별 제출/채점 1행. MyBatis가 채우고 service가 graded·file 보강 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Submission {
        private Long submissionId;       // 미제출(제출행 없음)이면 null
        private Long memberId;
        private String studentName;
        private String studentNo;        // MEMBER.LOGIN_ID
        private String submittedAt;      // "MM.DD HH:mm" (미제출 null)
        private String submissionStatus; // LEC_ASN_SBM_STATUS (NSB 등)
        private BigDecimal score;        // null = 미채점
        private String feedback;         // 없으면 "" (service에서 null → "")
        private boolean graded;          // score != null (service 계산)
        private SubmissionFile file;     // 미제출/무첨부면 null (service 보강)
    }

    /** 제출 파일 메타 (FE 단일 file). 첨부는 스키마상 1:N이라 최신 1건만 노출 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionFile {
        private String fileName;    // ASN_SBM_ATT_ORG_FIL_NAME
        private Long fileSize;      // ASN_SBM_ATT_FIL_SIZE (bytes)
        private String fileUrl;     // 인증 다운로드 엔드포인트 경로
        private String contentType; // ASN_SBM_ATT_EXT_TYPE (확장자)
    }

    /** 첨부파일 MyBatis row (service 내부 가공용) */
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

    /** 점수·피드백 저장 요청 (PUT) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveRequest {
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

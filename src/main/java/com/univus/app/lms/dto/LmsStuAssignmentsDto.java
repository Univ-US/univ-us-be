package com.univus.app.lms.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public final class LmsStuAssignmentsDto {

    private LmsStuAssignmentsDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentFlatRow {
        private Long semId;               // SEMESTERS.SEM_ID (학기별 페이지 조회 키)
        private Integer semYear;          // SEMESTERS.SEM_YEAR
        private String semTerm;           // SEMESTERS.SEM_TERM
        private Long assignmentId;        // LECTURE_ASSIGNMENT.LEC_ASN_ID (PK)
        private Long lecId;               // LECTURE.LEC_ID
        private String courseName;        // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;       // LECTURE.LEC_SECTION (분반)
        private String professor;         // MEMBER.MEMBER_NAME (조인)
        private String lecAsnTitle;       // LECTURE_ASSIGNMENT.LEC_ASN_TITLE
        private String lecAsnContent;     // LECTURE_ASSIGNMENT.LEC_ASN_CONTENT
        private String lecAsnDueDate;     // LECTURE_ASSIGNMENT.LEC_ASN_DUE_DATE
        private Integer overdueFlag;      // 파생(마감 경과 / CLS·NOP)
        private Long submissionId;        // LECTURE_ASSIGNMENT_SUBMISSION.LEC_ASN_SBM_ID (PK)
        private String lecAsnSbmStatus;   // LEC_ASN_SBM_STATUS (raw)
        private String lecAsnSbmRegDate;  // LEC_ASN_SBM_REG_DATE
        private String lecAsnSbmMemo;     // LEC_ASN_SBM_MEMO
        private String fileName;          // ASN_SBM_ATT_ORG_FIL_NAME (첨부 메타)
        private Long fileSize;            // ASN_SBM_ATT_FIL_SIZE (첨부 메타)
        private String fileExt;           // ASN_SBM_ATT_EXT_TYPE (첨부 메타)
        private BigDecimal asnSbmEvlScore;    // ASN_SBM_EVL_SCORE
        private String asnSbmEvlFeedback;     // ASN_SBM_EVL_FEEDBACK
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmittableRow {
        private Integer semYear;          // SEMESTERS.SEM_YEAR (조인)
        private String semTerm;           // SEMESTERS.SEM_TERM (조인)
        private Long assignmentId;        // LEC_ASN_ID (PK)
        private Long lecId;               // LECTURE.LEC_ID (조인)
        private String courseName;        // LEC_COD_NAME (조인)
        private Integer lecSection;       // LECTURE.LEC_SECTION (분반)
        private String professor;         // MEMBER_NAME (조인)
        private Long professorLmsPrfId;   // LECTURE.LMS_PRF_ID (아바타 색 시드 — 같은 교수=같은 색)
        private String professorImageUrl; // 교수 프로필 이미지 URL (LMS_PROFILE_IMAGE 최신 ACT, 없으면 null)
        private String lecAsnTitle;       // LECTURE_ASSIGNMENT.LEC_ASN_TITLE
        private String lecAsnContent;     // LECTURE_ASSIGNMENT.LEC_ASN_CONTENT
        private String lecAsnRegDate;     // LEC_ASN_REG_DATE TO_CHAR (작성자 행 등록일시)
        private String dueLabel;          // LEC_ASN_DUE_DATE TO_CHAR (파생 포맷)
        private String dueIso;            // LEC_ASN_DUE_DATE TO_CHAR ISO (파생 포맷)
        private String lecAsnValStatus;   // LECTURE_ASSIGNMENT.LEC_ASN_VAL_STATUS
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitAccessRow {
        private Long assignmentId;        // LEC_ASN_ID (PK)
        private String lecAsnValStatus;   // LECTURE_ASSIGNMENT.LEC_ASN_VAL_STATUS
        private Integer overdueFlag;      // 파생(마감 경과)
        private Long submissionId;        // LEC_ASN_SBM_ID (PK)
        private String lecAsnSbmStatus;   // LEC_ASN_SBM_STATUS
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitParam {
        private Long submissionId;
        private Long assignmentId;
        private Long lmsPrfId;
        private String memo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileRow {
        private Long submissionId;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private String trnFileName;
        private String orgUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentsResultResDto {
        private List<SemesterAssignmentsResDto> semesters;
    }

    /* ===== 학기별 과제 페이지네이션 (카드 헤더는 요약, 과제는 학기별 페이지 조회) ===== */

    /** 학기별 과제 요약 매핑 row (상태 필터 적용 GROUP BY) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentSemesterSummaryRow {
        private Long semId;
        private Integer semYear;
        private String semTerm;
        private int assignmentCount;
    }

    /** 학기별 과제 요약 응답 (카드 헤더 — 과제는 GET /assignments/semesters/{semId} 페이지 조회) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentSemesterSummaryResDto {
        private Long semId;
        private Integer semYear;
        private String semTerm;
        private String semesterLabel;
        private int assignmentCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterAssignmentsResDto {
        private Integer semYear;
        private String semTerm;
        private String semesterLabel;
        private List<StudentAssignmentResDto> assignments;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentAssignmentResDto {
        private Long id;
        private Long submissionId;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private String lecAsnTitle;
        private String lecAsnContent;
        private String lecAsnDueDate;
        private String status;            // 파생(NSB/SBM/GRD = resolveStatus) — 의미명 유지
        private boolean overdue;
        private BigDecimal asnSbmEvlScore;
        private Integer maxScore;
        private String lecAsnSbmRegDate;
        private String lecAsnSbmMemo;
        private SubmissionFileResDto file;
        private AssignmentFeedbackResDto feedback;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionFileResDto {
        private String fileName;
        private Long fileSize;
        private String fileUrl;
        private String contentType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentFeedbackResDto {
        private BigDecimal asnSbmEvlScore;
        private Integer maxScore;
        private String courseName;
        private String professor;
        private String asnSbmEvlFeedback;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmittableAssignmentResDto {
        private Long id;
        private Integer semYear;          // SEMESTERS.SEM_YEAR (년도 필터)
        private String semTerm;           // SEMESTERS.SEM_TERM (학기 필터)
        private Long lecId;               // LECTURE.LEC_ID (강의 드롭다운 키)
        private Integer lecSection;       // LECTURE.LEC_SECTION (분반)
        private String lecAsnTitle;
        private String lecAsnContent;     // 과제 설명(설명 영역 — 설명만 표기)
        private String lecAsnRegDate;     // 과제 등록일시(작성자 행 — 교수명 옆)
        private String courseName;
        private String dueLabel;          // 파생 포맷
        private String status;            // 파생(OPEN/EXTENDED)
        private String dDay;              // 파생
        private String note;              // 파생
        private String badge;             // 파생
        private SubmitGuideResDto guide;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitGuideResDto {
        private String courseName;
        private String professor;
        private Long professorLmsPrfId;   // 아바타 색 시드(lmsAvatar — 같은 교수=같은 색)
        private String professorImageUrl; // 교수 프로필 이미지 URL (없으면 null → 이니셜 폴백)
        private List<String> lines;
    }

    /** 제출 가능 과제 요약 — 사이드바 배지(전역 미제출 수) + 연도 필터 드롭다운 소스 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmittableSummaryResDto {
        private long totalCount;        // 전역(필터 무관) 제출 가능 과제 수
        private List<Integer> years;    // 제출 가능 과제가 존재하는 연도(내림차순)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitReqDto {
        private MultipartFile file;

        @Size(max = 1000, message = "제출 메모는 1000자 이내여야 합니다.")
        private String memo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSubmissionReqDto {
        private MultipartFile file;
        private Boolean removeExistingFile;

        @Size(max = 1000, message = "제출 메모는 1000자 이내여야 합니다.")
        private String memo;
    }
}

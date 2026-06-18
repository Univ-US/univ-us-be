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
        private Long assignmentId;
        private String courseName;
        private String professor;
        private String title;
        private String content;
        private String dueLabel;
        private String dueIso;
        private String valStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitAccessRow {
        private Long assignmentId;
        private String valStatus;
        private Integer overdueFlag;
        private Long submissionId;
        private String submissionStatus;
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
        private String title;
        private String courseName;
        private String dueLabel;
        private String status;
        private String dDay;
        private String note;
        private String badge;
        private String dotColor;
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
        private List<String> lines;
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

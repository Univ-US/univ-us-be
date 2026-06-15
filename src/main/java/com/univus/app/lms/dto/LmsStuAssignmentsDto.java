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
        private Integer year;
        private String termCode;
        private Long assignmentId;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private String professor;
        private String title;
        private String content;
        private String dueDate;
        private Integer overdueFlag;
        private Long submissionId;
        private String submissionStatus;
        private String submittedAt;
        private String submissionMemo;
        private String fileName;
        private Long fileSize;
        private String fileExt;
        private BigDecimal score;
        private String feedback;
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
        private Integer year;
        private String termCode;
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
        private String title;
        private String content;
        private String dueDate;
        private String status;
        private boolean overdue;
        private BigDecimal score;
        private Integer maxScore;
        private String submittedAt;
        private String submissionMemo;
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
        private BigDecimal score;
        private Integer maxScore;
        private String courseName;
        private String professor;
        private String comment;
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

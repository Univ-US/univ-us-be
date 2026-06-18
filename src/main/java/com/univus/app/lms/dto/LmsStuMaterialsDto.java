package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** SLM-006 학생 강의 자료 DTO */
public final class LmsStuMaterialsDto {

    private LmsStuMaterialsDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialFlatRow {
        private Integer semYear;          // SEMESTERS.SEM_YEAR
        private String semTerm;           // SEMESTERS.SEM_TERM
        private Long lecId;               // LECTURE.LEC_ID
        private String courseName;        // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;       // LECTURE.LEC_SECTION (분반)
        private Long uploadId;            // LECTURE_UPLOADING.LEC_UPL_ID (PK)
        private String lecUplTitle;       // LECTURE_UPLOADING.LEC_UPL_TITLE
        private String lecUplContent;     // LECTURE_UPLOADING.LEC_UPL_CONTENT
        private String lecUplRegDate;     // LEC_UPL_REG_DATE (TO_CHAR 'YYYY-MM-DD')
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long uploadId;
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDownloadRow {
        private Long attachmentId;
        private String fileName;
        private String trnFileName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentResDto {
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialResDto {
        private Long uploadId;
        private String lecUplTitle;
        private String lecUplContent;
        private String lecUplRegDate;
        private List<AttachmentResDto> attachments;
        private boolean downloadable;     // 파생
        private String lockedReason;      // 파생
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseMaterialsResDto {
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private List<MaterialResDto> materials;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterMaterialsResDto {
        private Integer semYear;
        private String semTerm;
        private String semesterLabel;
        private List<CourseMaterialsResDto> courses;
    }
}

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
        private Integer year;
        private String termCode;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Long uploadId;
        private String title;
        private String content;
        private String uploadedAt;
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
        private String title;
        private String content;
        private String uploadedAt;
        private List<AttachmentResDto> attachments;
        private boolean downloadable;
        private String lockedReason;
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
        private Integer year;
        private String termCode;
        private String semesterLabel;
        private List<CourseMaterialsResDto> courses;
    }
}

package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public final class LmsStuNoticeDto {

    private LmsStuNoticeDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeRow {
        private Long id;
        private Integer year;
        private String termCode;
        private Long lecId;
        private Integer lecSection;
        private String courseName;
        private String courseFullName;
        private String title;
        private String content;
        private String author;
        private String authorImageUrl;
        private String regDate;
        private String listDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long noticeId;
        private Long attachmentId;
        private String fileName;
        private Long fileSize;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDownloadRow {
        private String trnFileName;
        private String fileName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeAttachmentResDto {
        private Long attachmentId;
        private String fileName;
        private Long fileSize;
        private String downloadUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeResDto {
        private Long id;
        private Integer year;
        private String termCode;
        private String semesterLabel;
        private Long lecId;
        private Integer lecSection;
        private String courseName;
        private String courseFullName;
        private String title;
        private String author;
        private String authorImageUrl;
        private String date;
        private String listDate;
        private Boolean featured;
        private String content;
        private List<NoticeAttachmentResDto> attachments;
    }
}

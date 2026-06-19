package com.univus.app.serviceadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminCommunityDto {

    private ServiceAdminCommunityDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostSearch {
        private String keyword;
        private Long boardId;
        private String blindFilter;
        private String reportFilter;
        private Long univId;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Post {
        private Long postId;
        private Long memberId;
        private Long univId;
        private String authorName;
        private String authorNickname;
        private Long boardId;
        private String boardName;
        private String title;
        private Integer viewCount;
        private Integer reportCount;
        private Integer isBlind;
        private LocalDateTime createdAt;
    }

    @Getter
    public static class PostPage {
        private final List<Post> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public PostPage(List<Post> content, int page, int size, long totalElements) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BlindRequest {
        @NotNull
        private Boolean blind;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSearch {
        private String keyword;
        private String status;
        private Long univId;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberReportSummary {
        private Long memberId;
        private Long univId;
        private String memberName;
        private String loginId;
        private String role;
        private String univName;
        private String status;
        private Long reportedPostCount;
        private Long totalReportCount;
    }

    @Getter
    public static class MemberPage {
        private final List<MemberReportSummary> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public MemberPage(List<MemberReportSummary> content, int page, int size, long totalElements) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StatusChangeRequest {
        @NotBlank
        private String status;
    }
}

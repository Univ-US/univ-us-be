package com.univus.app.serviceadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminMemberDto {

    private ServiceAdminMemberDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String keyword;
        private String status;
        private String sort;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {
        private Long memberId;
        private Long univId;
        private String univName;
        private String loginId;
        private String memberName;
        private Long phoneNumber;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime logtimeAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private Long totalCount;
        private Long activeCount;
        private Long suspendedCount;
    }

    @Getter
    public static class Page {
        private final List<Member> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
        private final long totalCount;
        private final long activeCount;
        private final long suspendedCount;

        public Page(
                List<Member> content,
                int page,
                int size,
                long totalElements,
                Stats stats
        ) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages =
                    size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
            this.totalCount =
                    stats == null || stats.getTotalCount() == null
                            ? 0 : stats.getTotalCount();
            this.activeCount =
                    stats == null || stats.getActiveCount() == null
                            ? 0 : stats.getActiveCount();
            this.suspendedCount =
                    stats == null || stats.getSuspendedCount() == null
                            ? 0 : stats.getSuspendedCount();
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

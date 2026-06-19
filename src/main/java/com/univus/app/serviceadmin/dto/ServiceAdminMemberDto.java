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
    public static class UserSearch {
        private String keyword;
        private Long univId;
        private String role;
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
        private Long withdrawnCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private Long memberId;
        private Long univId;
        private String univName;
        private Long deptId;
        private String deptName;
        private String loginId;
        private String memberName;
        private String role;
        private Long phoneNumber;
        private String gender;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime logtimeAt;
        private String communityNickname;
        private String birth;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long totalCount;
        private Long activeCount;
        private Long suspendedCount;
        private Long withdrawnCount;
        private Long studentCount;
        private Long professorCount;
        private Long alumniCount;
        private Long guestCount;
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
        private final long withdrawnCount;

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
            this.withdrawnCount =
                    stats == null || stats.getWithdrawnCount() == null
                            ? 0 : stats.getWithdrawnCount();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class MemberDetail {
        private final Member member;
        private final MemberActivitySummary summary;
        private final List<UserLoginLog> loginLogs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberActivitySummary {
        private Long noticeCount;
        private Long postCount;
        private Long commentCount;
        private Long inquiryCount;
    }

    @Getter
    public static class UserPage {
        private final List<User> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
        private final long totalCount;
        private final long activeCount;
        private final long suspendedCount;
        private final long withdrawnCount;
        private final long studentCount;
        private final long professorCount;
        private final long alumniCount;
        private final long guestCount;

        public UserPage(
                List<User> content,
                int page,
                int size,
                long totalElements,
                UserStats stats
        ) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages =
                    size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
            this.totalCount = valueOrZero(stats == null ? null : stats.getTotalCount());
            this.activeCount = valueOrZero(stats == null ? null : stats.getActiveCount());
            this.suspendedCount = valueOrZero(stats == null ? null : stats.getSuspendedCount());
            this.withdrawnCount = valueOrZero(stats == null ? null : stats.getWithdrawnCount());
            this.studentCount = valueOrZero(stats == null ? null : stats.getStudentCount());
            this.professorCount = valueOrZero(stats == null ? null : stats.getProfessorCount());
            this.alumniCount = valueOrZero(stats == null ? null : stats.getAlumniCount());
            this.guestCount = valueOrZero(stats == null ? null : stats.getGuestCount());
        }

        private long valueOrZero(Long value) {
            return value == null ? 0 : value;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class UserDetail {
        private final User user;
        private final UserActivitySummary summary;
        private final List<UserLoginLog> loginLogs;
        private final List<UserActivityItem> communityActivities;
        private final List<UserActivityItem> courseActivities;
        private final List<UserActivityItem> submissionActivities;
        private final List<UserActivityItem> reservationActivities;
        private final List<UserActivityItem> inquiryActivities;
    }

    @Getter
    public static class UserLoginLogPage {
        private final List<UserLoginLog> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public UserLoginLogPage(
                List<UserLoginLog> content,
                int page,
                int size,
                long totalElements
        ) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages =
                    size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
        }
    }

    @Getter
    public static class UserActivityItemPage {
        private final List<UserActivityItem> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public UserActivityItemPage(
                List<UserActivityItem> content,
                int page,
                int size,
                long totalElements
        ) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages =
                    size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivitySummary {
        private Long postCount;
        private Long commentCount;
        private Long courseCount;
        private Long submissionCount;
        private Long reservationCount;
        private Long noShowCount;
        private Long inquiryCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLoginLog {
        private Long logId;
        private String result;
        private String failReason;
        private LocalDateTime logTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityItem {
        private String id;
        private String type;
        private String title;
        private String description;
        private String status;
        private LocalDateTime occurredAt;
    }

    @Getter
    @AllArgsConstructor
    public static class ForceLogoutResponse {
        private final Long memberId;
        private final int revokedSessionCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StatusChangeRequest {
        @NotBlank
        private String status;
    }
}

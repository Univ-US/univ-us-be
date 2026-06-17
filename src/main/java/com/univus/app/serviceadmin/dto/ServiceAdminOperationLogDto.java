package com.univus.app.serviceadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminOperationLogDto {

    private ServiceAdminOperationLogDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String keyword;
        private String category;
        private String result;
        private Integer periodDays;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Log {
        private String id;
        private String category;
        private String action;
        private String result;
        private LocalDateTime occurredAt;
        private Long sourceId;
        private Long memberId;
        private String memberName;
        private String loginId;
        private String role;
        private String memberStatus;
        private Long univId;
        private String univName;
        private String identifier;
        private String failReason;
        private String failReasonLabel;
        private Long subscriptionId;
        private String paymentStatus;
        private String merchantUid;
        private String portonePaymentId;
        private String portoneScheduleId;
        private Long amount;
        private String planName;
        private LocalDateTime paidAt;
        private LocalDateTime nextBillingAt;
        private LocalDateTime refundedAt;
        private Long refundAmount;
        private String refundReason;
        private String portoneCancellationId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long totalCount;
        private Long successCount;
        private Long failureCount;
        private Long scheduledCount;
    }

    @Getter
    public static class Page {
        private final List<Log> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
        private final long totalCount;
        private final long successCount;
        private final long failureCount;
        private final long scheduledCount;

        public Page(
                List<Log> content,
                int page,
                int size,
                long totalElements,
                Summary summary
        ) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages =
                    size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
            this.totalCount = value(summary == null ? null : summary.getTotalCount());
            this.successCount = value(summary == null ? null : summary.getSuccessCount());
            this.failureCount = value(summary == null ? null : summary.getFailureCount());
            this.scheduledCount = value(summary == null ? null : summary.getScheduledCount());
        }

        private long value(Long value) {
            return value == null ? 0 : value;
        }
    }
}

package com.univus.app.serviceadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminPaymentDto {

    private ServiceAdminPaymentDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String keyword;
        private String status;
        private Long planId;
        private String method;
        private String sort;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private Long historyId;
        private Long subscriptionId;
        private Long univId;
        private String univName;
        private Long memberId;
        private Long billingKeyId;
        private Long planId;
        private String planName;
        private String billingCycle;
        private Long amount;
        private String status;
        private String paymentMethod;
        private String paymentType;
        private String merchantUid;
        private String portonePaymentId;
        private String portoneScheduleId;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private String failReason;
        private LocalDateTime nextBillingAt;
        private String subscriptionStatus;
        private String pendingAction;
        private LocalDateTime cancellationEffectiveAt;
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
        private Long currentMonthRevenue;
        private Long currentMonthPaidCount;
        private Long currentMonthFailedCount;
        private Long currentMonthReadyCount;
        private Long readyPaymentAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanOption {
        private Long planId;
        private String planName;
    }

    @Getter
    public static class Page {
        private final List<Payment> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
        private final long currentMonthRevenue;
        private final long currentMonthPaidCount;
        private final long currentMonthFailedCount;
        private final long currentMonthReadyCount;
        private final long readyPaymentAmount;
        private final List<PlanOption> plans;

        public Page(
                List<Payment> content,
                int page,
                int size,
                long totalElements,
                Summary summary,
                List<PlanOption> plans
        ) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages =
                    size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
            this.currentMonthRevenue = value(summary == null
                    ? null : summary.getCurrentMonthRevenue());
            this.currentMonthPaidCount = value(summary == null
                    ? null : summary.getCurrentMonthPaidCount());
            this.currentMonthFailedCount = value(summary == null
                    ? null : summary.getCurrentMonthFailedCount());
            this.currentMonthReadyCount = value(summary == null
                    ? null : summary.getCurrentMonthReadyCount());
            this.readyPaymentAmount = value(summary == null
                    ? null : summary.getReadyPaymentAmount());
            this.plans = plans;
        }

        private long value(Long value) {
            return value == null ? 0 : value;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationTarget {
        private Long historyId;
        private Long subscriptionId;
        private Long univId;
        private Long memberId;
        private Long billingKeyId;
        private String portoneBillingKey;
        private String billingKeyStatus;
        private String planName;
        private String billingCycle;
        private Long amount;
        private String status;
        private String merchantUid;
        private String portonePaymentId;
        private String portoneScheduleId;
        private String subscriptionStatus;
        private String pendingAction;
        private Long currentReadyHistoryId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NewPayment {
        private Long historyId;
        private Long subscriptionId;
        private Long univId;
        private Long memberId;
        private Long billingKeyId;
        private String merchantUid;
        private String portoneScheduleId;
        private Long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RefundRequest {
        @NotBlank
        private String reason;
    }
}

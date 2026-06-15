package com.univus.app.serviceadmin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceAdminSchoolDto {

    private ServiceAdminSchoolDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String keyword;
        private String subscriptionStatus;
        private Long planId;
        private String sort;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class School {
        private Long univId;
        private String univName;
        private String sido;
        private String address;
        private String schoolPhone;
        private String homepage;
        private Long subscriptionId;
        private Long planId;
        private String planName;
        private Long planPrice;
        private String subscriptionStatus;
        private LocalDateTime firstPaidAt;
        private LocalDateTime startedAt;
        private LocalDateTime nextBillingAt;
        private LocalDateTime endedAt;
        private Long memberCount;
        private Long currentMonthRevenue;
        private String currentMonthPaymentStatus;
        private String nextPaymentStatus;
        private String pendingAction;
        private LocalDateTime pendingActionAt;
        private LocalDateTime cancellationEffectiveAt;
        private List<Admin> admins = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Admin {
        private Long memberId;
        private Long univId;
        private String loginId;
        private String memberName;
        private String role;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime logtimeAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Plan {
        private Long planId;
        private String planName;
        private Long price;
        private String billingCycle;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MutationTarget {
        private Long subscriptionId;
        private Long univId;
        private Long memberId;
        private Long billingKeyId;
        private String portoneBillingKey;
        private Long planId;
        private String planName;
        private Long planPrice;
        private String subscriptionStatus;
        private LocalDateTime nextBillingAt;
        private String pendingAction;
        private Long readyHistoryId;
        private String readyMerchantUid;
        private String readyScheduleId;
        private Long readyAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduledPayment {
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
    public static class PlanChangeRequest {
        @NotNull
        private Long planId;
    }
}

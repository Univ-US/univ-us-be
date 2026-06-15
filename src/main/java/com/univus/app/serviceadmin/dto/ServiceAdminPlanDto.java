package com.univus.app.serviceadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminPlanDto {

    private ServiceAdminPlanDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Plan {
        private Long planId;
        private String planName;
        private Long price;
        private String description;
        private String billingCycle;
        private Long maxMemberCount;
        private LocalDateTime createdAt;
        private LocalDateTime updateAt;
        private LocalDateTime deletedAt;
        private Long subscriberCount;

        public String getStatus() {
            return deletedAt == null ? "ACTIVE" : "INACTIVE";
        }
    }

    @Getter
    public static class Response {
        private final List<Plan> plans;
        private final long totalCount;
        private final long activeCount;
        private final long currentMonthRevenue;

        public Response(List<Plan> plans, Long currentMonthRevenue) {
            this.plans = plans;
            this.totalCount = plans.size();
            this.activeCount = plans.stream()
                    .filter(plan -> "ACTIVE".equals(plan.getStatus()))
                    .count();
            this.currentMonthRevenue =
                    currentMonthRevenue == null ? 0 : currentMonthRevenue;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SaveRequest {
        private Long planId;

        @NotBlank
        @Size(max = 10, message = "플랜명은 10자 이내로 입력해주세요.")
        private String planName;

        @NotNull
        @Positive
        private Long price;

        @NotBlank
        @Size(max = 300)
        private String description;

        @NotNull
        @Positive
        private Long maxMemberCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StatusRequest {
        @NotBlank
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadyPaymentTarget {
        private Long historyId;
        private Long subscriptionId;
        private Long memberId;
        private Long billingKeyId;
        private String portoneBillingKey;
        private String billingKeyStatus;
        private String merchantUid;
        private String portoneScheduleId;
        private Long amount;
        private LocalDateTime nextBillingAt;
        private String planName;
    }
}

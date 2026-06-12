package com.univus.app.serviceadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminDashboardDto {

    private ServiceAdminDashboardDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Summary summary;
        private List<SchoolOverview> schools;
        private List<RecentMember> recentMembers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long currentMonthRevenue;
        private Long previousMonthRevenue;
        private BigDecimal revenueChangeRate;
        private Long activeSchoolCount;
        private Long totalSchoolCount;
        private Long totalMemberCount;
        private Long currentMonthNewMemberCount;
        private Long previousMonthNewMemberCount;
        private BigDecimal newMemberChangeRate;
        private Long unresolvedInquiryCount;
        private Long pendingSchoolCount;
        private Long failedPaymentSchoolCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchoolOverview {
        private Long univId;
        private String univName;
        private String planName;
        private String subscriptionStatus;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private LocalDateTime nextBillingAt;
        private Long memberCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentMember {
        private Long memberId;
        private Long univId;
        private String univName;
        private String loginId;
        private String memberName;
        private String role;
        private String status;
        private LocalDateTime createdAt;
    }
}

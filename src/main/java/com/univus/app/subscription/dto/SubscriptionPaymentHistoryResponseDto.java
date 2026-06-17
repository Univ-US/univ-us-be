package com.univus.app.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPaymentHistoryResponseDto {

    private Long historyId;
    private String planName;
    private String billingCycle;
    private Long amount;
    private String status;
    private String paymentMethod;
    private String paymentType;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String failReason;
    private LocalDateTime nextBillingAt;
    private LocalDateTime refundedAt;
    private Long refundAmount;
}

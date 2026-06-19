package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionMutationTargetDto {

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

package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionRenewalTargetDto {

    private Long paymentHistoryId;
    private Long subscriptionId;
    private Long billingKeyId;
    private Long memberId;
    private Long univId;
    private Long amount;
    private String merchantUid;
    private String paymentStatus;
    private String subscriptionStatus;
    private String portoneBillingKey;
    private String billingKeyStatus;
    private String planName;
    private String billingCycle;
}

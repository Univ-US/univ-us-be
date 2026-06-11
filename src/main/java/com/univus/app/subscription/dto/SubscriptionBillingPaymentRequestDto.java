package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionBillingPaymentRequestDto {

    private String merchantUid;
    private String billingKey;
    private String paymentMethod;
}

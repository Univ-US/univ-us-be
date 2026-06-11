package com.univus.app.subscription.service;

import com.univus.app.subscription.dto.SubscriptionBillingPaymentRequestDto;
import com.univus.app.subscription.dto.SubscriptionPaymentVerifyResponseDto;
import com.univus.app.subscription.dto.SubscriptionWebhookRequestDto;

public interface SubscriptionBillingService {

    SubscriptionPaymentVerifyResponseDto completeInitialBillingPayment(
            Long memberId,
            SubscriptionBillingPaymentRequestDto request
    );

    void handleWebhook(SubscriptionWebhookRequestDto request);
}

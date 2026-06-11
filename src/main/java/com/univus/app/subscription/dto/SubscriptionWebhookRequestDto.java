package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionWebhookRequestDto {

    private String type;
    private String timestamp;
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {

        private String storeId;
        private String paymentId;
        private String transactionId;
        private String billingKey;
    }
}

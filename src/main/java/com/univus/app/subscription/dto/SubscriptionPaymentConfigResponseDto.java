package com.univus.app.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPaymentConfigResponseDto {

    private String storeId;
    private String channelKey;
    private String cardBillingChannelKey;
    private String kakaoBillingChannelKey;
}

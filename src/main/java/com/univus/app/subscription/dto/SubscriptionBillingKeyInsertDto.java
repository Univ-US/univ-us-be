package com.univus.app.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionBillingKeyInsertDto {

    private Long billingKeyId;
    private Long memberId;
    private Long univId;
    private String billingKeyValue;
    private String pgProvider;
    private String status;
}

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
public class SubscriptionPaymentMethodResponseDto {
    // 등록된 결제 수단이 없으면 registered=false이고 나머지 필드는 모두 null입니다.

    private boolean registered;
    private String paymentMethodType;
    private String maskedCardNumber;
    private String cardIssuer;
    private String status;
}

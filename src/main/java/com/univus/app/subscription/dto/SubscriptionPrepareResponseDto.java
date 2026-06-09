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
public class SubscriptionPrepareResponseDto {
    // 결제 준비가 끝난 뒤 프론트에 내려주는 응답 DTO입니다.
    // 프론트는 여기서 받은 merchantUid와 amount로 PortOne 결제를 시작합니다.

    // 내부 구독 ID입니다.
    private Long subscriptionId;

    // 내부 결제 이력 ID입니다.
    private Long paymentHistoryId;

    // PortOne 결제 요청에 사용할 주문 번호입니다.
    private String merchantUid;

    // 실제 결제 요청 금액입니다.
    private Long amount;

    private Long planId;
    private String planName;
}
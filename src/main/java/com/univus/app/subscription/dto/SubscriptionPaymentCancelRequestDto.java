package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPaymentCancelRequestDto {

    // prepare API에서 생성된 주문번호입니다.
    // 결제창 취소/실패 시 이 값으로 READY 결제 이력을 찾습니다.
    private String merchantUid;

    // 결제 취소/실패 사유입니다.
    // 예: USER_CANCELLED, 결제창 취소, PortOne error message 등
    private String reason;
}
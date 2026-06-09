package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPaymentVerifyRequestDto {

    // prepare API에서 백엔드가 생성해서 프론트에 내려준 주문번호입니다.
    // 백엔드는 이 값으로 READY 상태의 결제 이력을 찾습니다.
    private String merchantUid;

    // PortOne 결제 완료 후 프론트가 PortOne SDK 응답에서 받는 결제 ID입니다.
    // 백엔드는 이 값으로 PortOne 서버 API에 실제 결제 내역을 조회합니다.
    private String portonePaymentId;
}
package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPaymentVerifyTargetDto {

    // SUBSCRIPTION_PAYMENT_HISTORIES.HISTORY_ID
    private Long paymentHistoryId;

    // SUBSCRIPTIONS.SUBSCRIPTION_ID
    private Long subscriptionId;

    // SUBSCRIPTION_PAYMENT_HISTORIES.MEMBER_ID
    // 로그인 사용자와 결제 이력 소유자가 같은지 검증할 때 사용합니다.
    private Long memberId;

    // SUBSCRIPTION_PAYMENT_HISTORIES.UNIV_ID
    private Long univId;

    // SUBSCRIPTIONS.PLAN_ID
    private Long planId;

    // SUBSCRIPTION_PAYMENT_HISTORIES.MERCHANT_UID
    // 프론트가 보낸 주문번호와 DB 주문번호를 매칭합니다.
    private String merchantUid;

    // SUBSCRIPTION_PAYMENT_HISTORIES.AMOUNT
    // PortOne에서 조회한 실제 결제 금액과 비교합니다.
    private Long amount;

    // SUBSCRIPTION_PAYMENT_HISTORIES.STATUS
    // READY 상태일 때만 검증을 진행합니다.
    private String paymentStatus;

    // SUBSCRIPTIONS.STATUS
    // PENDING 상태일 때만 ACTIVE로 변경합니다.
    private String subscriptionStatus;
}
package com.univus.app.subscription.service;

import com.univus.app.subscription.dto.*;

import java.util.List;

public interface SubscriptionService {

    // 구독 플랜 목록을 조회합니다.
    // 로그인하지 않은 사용자도 플랜을 볼 수 있게 할지 여부는 SecurityConfig에서 결정합니다.
    List<SubscriptionPlanResponseDto> getActivePlans();

    // 결제 시작 전에 내부 구독 데이터와 결제 이력을 READY 상태로 생성합니다.
    // 실제 결제 성공 처리는 이후 PortOne 검증 API에서 별도로 처리합니다.
    SubscriptionPrepareResponseDto prepareSubscription(Long memberId, SubscriptionPrepareRequestDto request);

    // PortOne 결제 완료 후 프론트가 호출하는 검증 메서드입니다.
    // merchantUid로 READY 결제 이력을 찾고,
    // PortOne 서버 API에서 실제 결제 금액/상태를 조회한 뒤,
    // 결제 이력은 PAID, 구독은 ACTIVE로 변경합니다.
    SubscriptionPaymentVerifyResponseDto verifySubscriptionPayment(
            Long memberId,
            SubscriptionPaymentVerifyRequestDto request
    );
    // PortOne 결제창에서 사용자가 결제를 취소하거나 결제 진행에 실패했을 때 호출합니다.
    // prepare 단계에서 미리 생성된 READY 결제 이력과 PENDING 구독을
    // CANCELED 상태로 닫아, 같은 사용자가 다시 구독 신청을 시도할 수 있게 합니다.
    void cancelPreparedSubscriptionPayment(
            Long memberId,
            SubscriptionPaymentCancelRequestDto request
    );
}
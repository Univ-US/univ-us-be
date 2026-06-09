package com.univus.app.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPaymentVerifyResponseDto {

    // 활성화된 구독 ID입니다.
    private Long subscriptionId;

    // PAID 상태로 변경된 결제 이력 ID입니다.
    private Long paymentHistoryId;

    // PortOne 결제 ID입니다.
    private String portonePaymentId;

    // 우리 서버에서 생성한 주문번호입니다.
    private String merchantUid;

    // 실제 결제 검증이 완료된 금액입니다.
    private Long amount;

    // 결제 이력 상태입니다.
    // 성공 시 PAID를 내려줍니다.
    private String paymentStatus;

    // 구독 상태입니다.
    // 성공 시 ACTIVE를 내려줍니다.
    private String subscriptionStatus;

    // 결제 완료 시각입니다.
    private LocalDateTime paidAt;

    // 다음 정기 결제 예정일입니다.
    private LocalDateTime nextBillingAt;

    // 결제 완료 후 관리자 권한으로 갱신된 회원 ID입니다.
    private Long memberId;

    // 결제 완료 후 회원에게 연결된 학교 ID입니다.
    private Long univId;

    // 결제 완료 후 변경된 회원 역할입니다.
    // 구독 결제 성공 시 ADM으로 내려줍니다.
    private String role;

    // ADM 권한이 반영된 새 accessToken입니다.
    // 프론트는 기존 GUEST accessToken을 이 값으로 교체해야 합니다.
    private String accessToken;

    // 토큰 타입입니다.
    // 일반적으로 Bearer입니다.
    private String tokenType;
}
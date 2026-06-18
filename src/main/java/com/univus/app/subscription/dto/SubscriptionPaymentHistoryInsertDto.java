package com.univus.app.subscription.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SUBSCRIPTION_PAYMENT_HISTORIES 테이블 insert용 DTO입니다.
// 결제 전에는 READY 상태로 저장하고, PortOne 검증 후 PAID 또는 FAILED로 변경합니다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPaymentHistoryInsertDto {

    private Long historyId;
    private Long subscriptionId;
    private Long univId;
    private Long memberId;

    // 빌링키는 결제수단 등록/검증 흐름에서 연결됩니다.
    // 준비 단계에서는 null일 수 있습니다.
    private Long billingKeyId;

    private String status;

    // PortOne에 넘길 주문 번호입니다. 우리 서버에서 먼저 생성해서 DB에 저장합니다.
    private String merchantUid;

    // 실제 PortOne 결제 ID입니다.
    // 결제 완료/검증 전에는 null입니다.
    private String portonePaymentId;

    private LocalDateTime paidAt;
    private String failReason;
    private Long amount;
    private String planName;

    // 예약결제(PortOne payment-schedule) 생성 시 받은 스케줄 ID입니다.
    // 즉시결제 흐름에서는 null입니다.
    private String portoneScheduleId;
}

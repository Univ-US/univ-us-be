package com.univus.app.subscription.dto;

import java.time.LocalDateTime;

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
public class SubscriptionInsertDto {
    // SUBSCRIPTIONS 테이블 insert용 DTO입니다.
    // 컨트롤러 요청 DTO와 DB insert DTO를 분리해서 요청 값과 저장 값을 섞지 않도록 합니다.

    private Long subscriptionId;
    private Long planId;

    // 빌링키는 실제 결제수단 등록이 끝난 뒤 연결됩니다.
    // 준비 단계에서는 null로 저장될 수 있습니다.
    private Long billingKeyId;

    private Long memberId;
    private Long univId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime nextBillingAt;
    private LocalDateTime endedAt;
}
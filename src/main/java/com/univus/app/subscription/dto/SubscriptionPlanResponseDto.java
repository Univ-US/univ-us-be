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
public class SubscriptionPlanResponseDto {
    // 구독 플랜 목록을 프론트에 내려줄 때 사용하는 응답 DTO입니다.

    private Long planId;
    private String planName;
    private Long price;
    private String description;
    private String billingCycle;
    private Long maxMemberCount;
}
package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPrepareRequestDto {
    // 사용자가 구독 신청 버튼을 눌렀을 때 BE로 보내는 요청 DTO입니다.
    // 이 단계에서는 실제 결제 완료가 아니라, 결제를 시작하기 위한 내부 데이터를 미리 생성합니다.

    // 사용자가 선택한 구독 플랜 ID입니다.
    private Long planId;
    private Long univId;

    // 결제 성공 후 ADM 계정과 연결될 학교 정보입니다.
    private String univName;
    private String sido;
    private String address;
    private String schoolPhone;
    private String homepage;
}

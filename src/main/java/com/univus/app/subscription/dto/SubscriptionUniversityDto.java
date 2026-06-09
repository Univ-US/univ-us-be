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
public class SubscriptionUniversityDto {
    // 구독 신청 과정에서 새로 생성할 학교 정보를 담는 DTO입니다.
    // mapper insert 후 selectKey로 univId가 채워집니다.

    private Long univId;
    private String univName;
    private String sido;
    private String address;
    private String schoolPhone;
    private String homepage;
}
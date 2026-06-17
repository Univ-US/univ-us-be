package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionUniversityOptionDto {

    private Long univId;
    private String univName;
    private String sido;
    private String address;
    private String schoolPhone;
    private String homepage;
    private String subscriptionStatus;
}

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
public class SubscriptionApplicationDto {

    private Long applicationId;
    private Long memberId;
    private Long subscriptionId;
    private Long planId;

    private String univName;
    private String sido;
    private String address;
    private String schoolPhone;
    private String homepage;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime failedAt;
    private String failReason;
}

package com.univus.app.subscription.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SubscriptionAccessStatusDto {

    private Long memberId;
    private String role;
    private Long univId;
    private String univName;
    private String sido;
    private String address;
    private String schoolPhone;
    private String homepage;
    private Long subscriptionId;
    private String subscriptionStatus;
    private String pendingAction;
    private LocalDateTime cancellationEffectiveAt;
    private LocalDateTime endedAt;
    private String accessStatus;
    private boolean serviceAccessible;
    private boolean resubscribeAvailable;
}

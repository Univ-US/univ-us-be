package com.univus.app.subscription.service;

import com.univus.app.subscription.dto.SubscriptionAccessStatusDto;

public interface SubscriptionAccessService {

    SubscriptionAccessStatusDto getStatus(Long memberId);
}

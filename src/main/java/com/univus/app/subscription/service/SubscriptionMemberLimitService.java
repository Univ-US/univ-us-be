package com.univus.app.subscription.service;

public interface SubscriptionMemberLimitService {

    void validateAdditionalMembers(Long univId, int additionalCount);
}

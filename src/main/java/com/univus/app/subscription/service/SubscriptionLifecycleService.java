package com.univus.app.subscription.service;

public interface SubscriptionLifecycleService {

    void changePlan(Long univId, Long planId);

    void scheduleCancellation(Long univId);

    void resumeSubscription(Long univId);
}

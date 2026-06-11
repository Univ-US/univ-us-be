package com.univus.app.subscription.service;

import com.univus.app.subscription.mapper.SubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentFailureRecorderTest {

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionPaymentFailureRecorder failureRecorder;

    @Test
    void markFailedClosesPaymentSubscriptionAndApplicationTogether() {
        when(subscriptionMapper.markPaymentHistoryFailed(300L, "verification failed"))
                .thenReturn(1);
        when(subscriptionMapper.cancelSubscription(100L)).thenReturn(1);
        when(subscriptionMapper.failSubscriptionApplication(200L, "verification failed"))
                .thenReturn(1);

        failureRecorder.markFailed(300L, 100L, 200L, "verification failed");

        InOrder order = inOrder(subscriptionMapper);
        order.verify(subscriptionMapper).markPaymentHistoryFailed(300L, "verification failed");
        order.verify(subscriptionMapper).cancelSubscription(100L);
        order.verify(subscriptionMapper).failSubscriptionApplication(200L, "verification failed");
    }
}

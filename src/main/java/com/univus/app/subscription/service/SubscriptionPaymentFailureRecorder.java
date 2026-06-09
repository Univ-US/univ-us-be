package com.univus.app.subscription.service;

import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionPaymentFailureRecorder {

    private final SubscriptionMapper subscriptionMapper;

    // 결제 검증 실패 이력은 verify 트랜잭션이 롤백돼도 DB에 남아야 합니다.
    // 그래서 기존 트랜잭션과 분리된 새 트랜잭션으로 FAILED 상태를 저장합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long historyId, String failReason) {
        subscriptionMapper.markPaymentHistoryFailed(historyId, failReason);
    }
}
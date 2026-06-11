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
    // 그래서 기존 트랜잭션과 분리된 새 트랜잭션으로 결제/구독/신청 상태를 함께 닫습니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            Long historyId,
            Long subscriptionId,
            Long applicationId,
            String failReason
    ) {
        requireSingleRow(
                subscriptionMapper.markPaymentHistoryFailed(historyId, failReason),
                "결제 실패 이력 저장에 실패했습니다."
        );

        requireSingleRow(
                subscriptionMapper.cancelSubscription(subscriptionId),
                "실패한 구독 정리에 실패했습니다."
        );

        if (applicationId != null) {
            requireSingleRow(
                    subscriptionMapper.failSubscriptionApplication(applicationId, failReason),
                    "구독 신청 실패 상태 저장에 실패했습니다."
            );
        }
    }

    private void requireSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new IllegalStateException(message);
        }
    }
}

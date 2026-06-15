package com.univus.app.subscription.service;

import com.univus.app.subscription.dto.SubscriptionAccessStatusDto;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionAccessService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING = "PENDING";
    private static final String CANCEL = "CANCEL";

    private final SubscriptionMapper subscriptionMapper;

    @Transactional(readOnly = true)
    public SubscriptionAccessStatusDto getStatus(Long memberId) {
        SubscriptionAccessStatusDto status =
                subscriptionMapper.findSubscriptionAccessStatus(memberId);

        if (status == null) {
            throw new IllegalArgumentException("Member information was not found.");
        }

        resolveAccessStatus(status, LocalDateTime.now());
        return status;
    }

    private void resolveAccessStatus(
            SubscriptionAccessStatusDto status,
            LocalDateTime now
    ) {
        if ("SUA".equals(status.getRole())) {
            setStatus(status, ACTIVE, true, false);
            return;
        }

        if (status.getUnivId() == null || status.getSubscriptionId() == null) {
            setStatus(status, "UNSUBSCRIBED", false, canSubscribe(status));
            return;
        }

        if (ACTIVE.equals(status.getSubscriptionStatus())) {
            if (CANCEL.equals(status.getPendingAction())) {
                LocalDateTime effectiveAt = status.getCancellationEffectiveAt();
                if (effectiveAt != null && !effectiveAt.isAfter(now)) {
                    setStatus(status, "EXPIRED", false, canSubscribe(status));
                    return;
                }
                setStatus(status, "CANCEL_SCHEDULED", true, false);
                return;
            }

            setStatus(status, ACTIVE, true, false);
            return;
        }

        if (PENDING.equals(status.getSubscriptionStatus())) {
            setStatus(status, PENDING, false, false);
            return;
        }

        setStatus(status, "EXPIRED", false, canSubscribe(status));
    }

    private boolean canSubscribe(SubscriptionAccessStatusDto status) {
        return "GUEST".equals(status.getRole()) || "ADM".equals(status.getRole());
    }

    private void setStatus(
            SubscriptionAccessStatusDto status,
            String accessStatus,
            boolean serviceAccessible,
            boolean resubscribeAvailable
    ) {
        status.setAccessStatus(accessStatus);
        status.setServiceAccessible(serviceAccessible);
        status.setResubscribeAvailable(resubscribeAvailable);
    }
}

package com.univus.app.subscription.service;

import com.univus.app.subscription.mapper.SubscriptionMemberLimitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SubscriptionMemberLimitService {

    private final SubscriptionMemberLimitMapper subscriptionMemberLimitMapper;

    @Transactional(readOnly = true)
    public void validateAdditionalMembers(Long univId, int additionalCount) {
        if (univId == null || additionalCount <= 0) {
            return;
        }

        Long maxMemberCount =
                subscriptionMemberLimitMapper.selectMaxMemberCount(univId);
        if (maxMemberCount == null) {
            return;
        }

        long currentCount =
                subscriptionMemberLimitMapper.countBillableMembers(univId);
        if (currentCount + additionalCount > maxMemberCount) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "구독 플랜의 최대 이용자 수를 초과합니다. 현재 "
                            + currentCount
                            + "명 / 최대 "
                            + maxMemberCount
                            + "명"
            );
        }
    }
}

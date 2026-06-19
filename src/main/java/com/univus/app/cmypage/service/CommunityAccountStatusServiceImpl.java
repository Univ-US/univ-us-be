package com.univus.app.cmypage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.cmypage.mapper.CmypageMapper;
import com.univus.app.reservation.service.PendingReservationCancellationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityAccountStatusServiceImpl
        implements CommunityAccountStatusService {

    private final CmypageMapper cmypageMapper;
    private final PendingReservationCancellationService
            pendingReservationCancellationService;

    @Transactional
    @Override
    public void deactivateCommunity(Long memberId) {
        cmypageMapper.updateMemberStatus(
                memberId,
                CmypageConstants.MEMBER_STATUS_INACTIVE);
        pendingReservationCancellationService
                .cancelAllPendingReservations(memberId);
    }

    @Transactional
    @Override
    public void reactivateCommunity(Long memberId) {
        cmypageMapper.updateMemberStatus(
                memberId,
                CmypageConstants.MEMBER_STATUS_ACTIVE);
    }
}

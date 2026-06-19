package com.univus.app.cmypage.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.univus.app.cmypage.mapper.CmypageMapper;
import com.univus.app.reservation.service.PendingReservationCancellationService;

class CommunityAccountStatusServiceImplTest {

    private final CmypageMapper cmypageMapper = mock(CmypageMapper.class);
    private final PendingReservationCancellationService
            pendingReservationCancellationService =
                    mock(PendingReservationCancellationService.class);
    private final CommunityAccountStatusService accountStatusService =
            new CommunityAccountStatusServiceImpl(
                    cmypageMapper,
                    pendingReservationCancellationService);

    @Test
    @DisplayName("커뮤니티 비활성화는 회원 상태 변경 후 대기 예약을 취소한다")
    void deactivateCommunityUpdatesStatusAndCancelsReservations() {
        Long memberId = 21L;

        accountStatusService.deactivateCommunity(memberId);

        InOrder order = inOrder(
                cmypageMapper,
                pendingReservationCancellationService);
        order.verify(cmypageMapper).updateMemberStatus(
                memberId,
                "INACTIVE");
        order.verify(pendingReservationCancellationService)
                .cancelAllPendingReservations(memberId);
    }

    @Test
    @DisplayName("커뮤니티 재활성화는 회원 상태를 활성으로 변경한다")
    void reactivateCommunityUpdatesStatus() {
        Long memberId = 22L;

        accountStatusService.reactivateCommunity(memberId);

        verify(cmypageMapper).updateMemberStatus(
                memberId,
                "ACTIVE");
    }
}

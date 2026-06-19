package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminReservationPenaltyDto;

public interface ServiceAdminReservationPenaltyService {

    ServiceAdminReservationPenaltyDto.PenaltyPage getPenalties(
            int page,
            Long memberId,
            String keyword,
            String status,
            Long requesterId
    );

    ServiceAdminReservationPenaltyDto.MemberPenaltyStatus getMemberStatus(Long memberId, Long requesterId);

    ServiceAdminReservationPenaltyDto.Penalty grantPenalty(Long memberId, String reason, Long requesterId);

    ServiceAdminReservationPenaltyDto.Penalty releasePenalty(Long penaltyId, Long requesterId);
}

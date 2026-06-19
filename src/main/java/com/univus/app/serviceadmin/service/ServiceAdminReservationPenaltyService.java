package com.univus.app.serviceadmin.service;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.reservation.mapper.ReservationMapper;
import com.univus.app.serviceadmin.dto.ServiceAdminReservationPenaltyDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminReservationPenaltyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAdminReservationPenaltyService {

    private static final int PAGE_SIZE = 10;
    private static final int PENALTY_BLOCK_THRESHOLD = 5;
    private static final String MANUAL_PENALTY_TYPE = "MANUAL";
    private static final String ROLE_ADM = "ADM";

    private final ServiceAdminReservationPenaltyMapper serviceAdminReservationPenaltyMapper;
    private final ReservationMapper reservationMapper;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public ServiceAdminReservationPenaltyDto.PenaltyPage getPenalties(
            int page,
            Long memberId,
            String keyword,
            String status,
            Long requesterId
    ) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);

        int safePage = Math.max(page, 0);
        ServiceAdminReservationPenaltyDto.PenaltySearch search = new ServiceAdminReservationPenaltyDto.PenaltySearch();
        search.setMemberId(memberId);
        search.setKeyword(normalizeKeyword(keyword));
        search.setStatus(normalizeKeyword(status));
        search.setUnivId(scopeUnivId);
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminReservationPenaltyDto.Penalty> penalties =
                serviceAdminReservationPenaltyMapper.selectPenalties(search);
        long totalElements = serviceAdminReservationPenaltyMapper.countPenalties(search);
        return new ServiceAdminReservationPenaltyDto.PenaltyPage(penalties, safePage, PAGE_SIZE, totalElements);
    }

    @Transactional(readOnly = true)
    public ServiceAdminReservationPenaltyDto.MemberPenaltyStatus getMemberStatus(Long memberId, Long requesterId) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);
        MemberDto member = requireMember(memberId, scopeUnivId);
        int activePenaltyCount = reservationMapper.countActiveReservationPenalties(memberId);
        return new ServiceAdminReservationPenaltyDto.MemberPenaltyStatus(
                memberId,
                member.getMemberName(),
                activePenaltyCount,
                PENALTY_BLOCK_THRESHOLD,
                activePenaltyCount >= PENALTY_BLOCK_THRESHOLD
        );
    }

    @Transactional
    public ServiceAdminReservationPenaltyDto.Penalty grantPenalty(Long memberId, String reason, Long requesterId) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);
        requireMember(memberId, scopeUnivId);
        reservationMapper.insertReservationPenalty(memberId, MANUAL_PENALTY_TYPE, reason);
        return serviceAdminReservationPenaltyMapper.selectLatestPenaltyForMember(memberId);
    }

    @Transactional
    public ServiceAdminReservationPenaltyDto.Penalty releasePenalty(Long penaltyId, Long requesterId) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);
        ServiceAdminReservationPenaltyDto.Penalty penalty =
                serviceAdminReservationPenaltyMapper.selectPenaltyById(penaltyId);
        if (penalty == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "패널티를 찾을 수 없습니다.");
        }
        requireMember(penalty.getMemberId(), scopeUnivId);
        if (!"ACTIVE".equals(penalty.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 패널티입니다.");
        }

        int updated = serviceAdminReservationPenaltyMapper.releasePenalty(penaltyId);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "패널티를 면제하지 못했습니다.");
        }
        return serviceAdminReservationPenaltyMapper.selectPenaltyById(penaltyId);
    }

    /**
     * SUA는 전체 학교, ADM은 본인 소속 학교(univId)로만 범위가 제한된다.
     */
    private Long resolveScopeUnivId(Long requesterId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return ROLE_ADM.equals(requester.getRole()) ? requester.getUnivId() : null;
    }

    private MemberDto requireMember(Long memberId, Long scopeUnivId) {
        MemberDto member = memberMapper.findByMemberId(memberId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        if (scopeUnivId != null && !scopeUnivId.equals(member.getUnivId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        return member;
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

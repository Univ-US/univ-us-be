package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceAdminMemberService {

    private static final int PAGE_SIZE = 10;
    private static final Set<String> FILTER_STATUSES =
            Set.of("ACTIVE", "SUSPENDED", "WITHDRAWN");
    private static final Set<String> MUTABLE_STATUSES =
            Set.of("ACTIVE", "SUSPENDED");
    private static final Set<String> SORT_OPTIONS =
            Set.of("SCHOOL_ASC", "NAME_ASC", "JOINED_DESC");

    private final ServiceAdminMemberMapper serviceAdminMemberMapper;

    @Transactional(readOnly = true)
    public ServiceAdminMemberDto.Page getMembers(
            int page,
            String keyword,
            String status,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminMemberDto.Search search = new ServiceAdminMemberDto.Search();
        search.setKeyword(normalizeKeyword(keyword));
        search.setStatus(normalizeFilterStatus(status));
        search.setSort(normalizeSort(sort));
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminMemberDto.Member> members =
                serviceAdminMemberMapper.selectMembers(search);
        long totalElements = serviceAdminMemberMapper.countMembers(search);
        ServiceAdminMemberDto.Stats stats =
                serviceAdminMemberMapper.selectMemberStats();

        return new ServiceAdminMemberDto.Page(
                members,
                safePage,
                PAGE_SIZE,
                totalElements,
                stats
        );
    }

    @Transactional
    public ServiceAdminMemberDto.Member changeStatus(
            Long memberId,
            String requestedStatus
    ) {
        String status = normalizeMutableStatus(requestedStatus);
        ServiceAdminMemberDto.Member member =
                serviceAdminMemberMapper.selectMemberById(memberId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "학교 관리자 계정을 찾을 수 없습니다."
            );
        }
        if ("WITHDRAWN".equals(member.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "탈퇴한 학교 관리자 계정의 상태는 변경할 수 없습니다."
            );
        }
        if (status.equals(member.getStatus())) {
            return member;
        }

        int updated = serviceAdminMemberMapper.updateMemberStatus(memberId, status);
        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "학교 관리자 계정 상태를 변경하지 못했습니다."
            );
        }

        return serviceAdminMemberMapper.selectMemberById(memberId);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeFilterStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!FILTER_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 회원 상태입니다."
            );
        }
        return normalized;
    }

    private String normalizeMutableStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!MUTABLE_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "회원 상태는 ACTIVE 또는 SUSPENDED로만 변경할 수 있습니다."
            );
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "SCHOOL_ASC";
        }
        String normalized = sort.trim().toUpperCase();
        if (!SORT_OPTIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다."
            );
        }
        return normalized;
    }
}

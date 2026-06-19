package com.univus.app.serviceadmin.service;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.serviceadmin.dto.ServiceAdminCommunityDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminCommunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceAdminCommunityService {

    private static final int PAGE_SIZE = 10;
    private static final Set<String> BLIND_FILTERS = Set.of("ALL", "BLIND", "VISIBLE");
    private static final Set<String> REPORT_FILTERS = Set.of("ALL", "REPORTED");
    private static final Set<String> MEMBER_FILTER_STATUSES = Set.of("ACTIVE", "SUSPENDED");
    private static final Set<String> MEMBER_MUTABLE_STATUSES = Set.of("ACTIVE", "SUSPENDED");
    private static final String ROLE_ADM = "ADM";

    private final ServiceAdminCommunityMapper serviceAdminCommunityMapper;
    private final MemberMapper memberMapper;

    @Transactional(readOnly = true)
    public ServiceAdminCommunityDto.PostPage getPosts(
            int page,
            String keyword,
            Long boardId,
            String blindFilter,
            String reportFilter,
            Long requesterId
    ) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);

        int safePage = Math.max(page, 0);
        ServiceAdminCommunityDto.PostSearch search = new ServiceAdminCommunityDto.PostSearch();
        search.setKeyword(normalizeKeyword(keyword));
        search.setBoardId(boardId);
        search.setBlindFilter(normalizeFilter(blindFilter, BLIND_FILTERS, "지원하지 않는 블라인드 필터입니다."));
        search.setReportFilter(normalizeFilter(reportFilter, REPORT_FILTERS, "지원하지 않는 신고 필터입니다."));
        search.setUnivId(scopeUnivId);
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminCommunityDto.Post> posts = serviceAdminCommunityMapper.selectPosts(search);
        long totalElements = serviceAdminCommunityMapper.countPosts(search);
        return new ServiceAdminCommunityDto.PostPage(posts, safePage, PAGE_SIZE, totalElements);
    }

    @Transactional
    public ServiceAdminCommunityDto.Post setPostBlind(Long postId, boolean blind, Long requesterId) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);
        requirePost(postId, scopeUnivId);
        serviceAdminCommunityMapper.updatePostBlind(postId, blind ? 1 : 0);
        return requirePost(postId, scopeUnivId);
    }

    @Transactional
    public void deletePost(Long postId, Long requesterId) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);
        requirePost(postId, scopeUnivId);
        int updated = serviceAdminCommunityMapper.softDeletePost(postId);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "게시글을 삭제하지 못했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public ServiceAdminCommunityDto.MemberPage getMembers(
            int page,
            String keyword,
            String status,
            Long requesterId
    ) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);

        int safePage = Math.max(page, 0);
        ServiceAdminCommunityDto.MemberSearch search = new ServiceAdminCommunityDto.MemberSearch();
        search.setKeyword(normalizeKeyword(keyword));
        search.setStatus(normalizeFilterStatus(status));
        search.setUnivId(scopeUnivId);
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminCommunityDto.MemberReportSummary> members = serviceAdminCommunityMapper.selectMembers(search);
        long totalElements = serviceAdminCommunityMapper.countMembers(search);
        return new ServiceAdminCommunityDto.MemberPage(members, safePage, PAGE_SIZE, totalElements);
    }

    @Transactional
    public ServiceAdminCommunityDto.MemberReportSummary changeMemberStatus(
            Long memberId,
            String requestedStatus,
            Long requesterId
    ) {
        Long scopeUnivId = resolveScopeUnivId(requesterId);
        String status = normalizeMutableStatus(requestedStatus);
        ServiceAdminCommunityDto.MemberReportSummary member = requireMember(memberId, scopeUnivId);
        if (status.equals(member.getStatus())) {
            return member;
        }

        int updated = serviceAdminCommunityMapper.updateMemberStatus(memberId, status);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "회원 상태를 변경하지 못했습니다.");
        }
        return serviceAdminCommunityMapper.selectMemberById(memberId);
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

    private ServiceAdminCommunityDto.Post requirePost(Long postId, Long scopeUnivId) {
        ServiceAdminCommunityDto.Post post = serviceAdminCommunityMapper.selectPostById(postId);
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        if (scopeUnivId != null && !scopeUnivId.equals(post.getUnivId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private ServiceAdminCommunityDto.MemberReportSummary requireMember(Long memberId, Long scopeUnivId) {
        ServiceAdminCommunityDto.MemberReportSummary member = serviceAdminCommunityMapper.selectMemberById(memberId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        if (scopeUnivId != null && !scopeUnivId.equals(member.getUnivId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        return member;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeFilter(String value, Set<String> allowed, String errorMessage) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return normalized;
    }

    private String normalizeFilterStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!MEMBER_FILTER_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 회원 상태입니다.");
        }
        return normalized;
    }

    private String normalizeMutableStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!MEMBER_MUTABLE_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원 상태는 ACTIVE 또는 SUSPENDED로만 변경할 수 있습니다.");
        }
        return normalized;
    }
}

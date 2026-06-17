package com.univus.app.community.service;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommunityAccessService {

    private static final String ROLE_SUPER_ADMIN = "SUA";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final MemberMapper memberMapper;

    public CommunityAccessScope getScope(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        MemberDto member = memberMapper.findByMemberId(memberId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        if (STATUS_SUSPENDED.equals(member.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정은 커뮤니티를 이용할 수 없습니다.");
        }

        if (ROLE_SUPER_ADMIN.equals(member.getRole())) {
            return CommunityAccessScope.superAdmin(member.getMemberId());
        }

        if (member.getUnivId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "소속 학교 정보가 없습니다.");
        }

        return CommunityAccessScope.schoolMember(member.getMemberId(), member.getUnivId());
    }

    public void assertAccessible(Long ownerUnivId, CommunityAccessScope scope) {
        if (scope == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (scope.isSuperAdmin()) {
            return;
        }
        if (ownerUnivId == null || !ownerUnivId.equals(scope.getUnivId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "커뮤니티 데이터를 찾을 수 없습니다.");
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    public static class CommunityAccessScope {
        private final Long memberId;
        private final Long univId;
        private final boolean superAdmin;

        public static CommunityAccessScope superAdmin(Long memberId) {
            return of(memberId, null, true);
        }

        public static CommunityAccessScope schoolMember(Long memberId, Long univId) {
            return of(memberId, univId, false);
        }

        public Long getMemberId() {
            return memberId;
        }

        public Long getUnivId() {
            return univId;
        }

        public boolean isSuperAdmin() {
            return superAdmin;
        }

        public Long getQueryUnivId() {
            return superAdmin ? null : univId;
        }
    }
}

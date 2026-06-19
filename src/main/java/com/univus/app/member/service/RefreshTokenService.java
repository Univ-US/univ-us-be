package com.univus.app.member.service;

import com.univus.app.member.dto.AdminSessionInfoDto;
import lombok.Getter;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenService {

    LoginSession createSession(
            String refreshToken,
            Long memberId,
            String role,
            String ipAddress,
            String userAgent,
            Duration ttl
    );

    Optional<LoginSession> findSessionByRefreshToken(String refreshToken);

    Optional<Long> findMemberId(String refreshToken);

    Optional<AdminSessionInfoDto> findCurrentAdminSession(Long memberId);

    boolean isCurrentAdminSession(Long memberId, String sessionId);

    boolean sessionExists(Long memberId, String sessionId);

    void rotate(String oldRefreshToken, String newRefreshToken, Duration ttl);

    void delete(String refreshToken);

    void deleteCurrentAdminSession(Long memberId);

    int deleteMemberSessions(Long memberId);

    @Getter
    class LoginSession {

        private final String sessionId;
        private final Long memberId;
        private final String role;

        public LoginSession(String sessionId, Long memberId, String role) {
            this.sessionId = sessionId;
            this.memberId = memberId;
            this.role = role;
        }
    }
}

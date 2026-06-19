package com.univus.app.member.service;

import com.univus.app.member.dto.AdminSessionInfoDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String ADMIN_SESSION_KEY_PREFIX = "auth:admin-session:";
    private static final String MEMBER_SESSION_KEY_PREFIX = "auth:member-sessions:";

    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_IP_ADDRESS = "ipAddress";
    private static final String FIELD_USER_AGENT = "userAgent";
    private static final String FIELD_LOGIN_AT = "loginAt";
    private static final String FIELD_REFRESH_KEY = "refreshKey";

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginSession createSession(
            String refreshToken,
            Long memberId,
            String role,
            String ipAddress,
            String userAgent,
            Duration ttl
    ) {
        String sessionId = UUID.randomUUID().toString();
        String refreshKey = refreshKey(refreshToken);
        String sessionKey = sessionKey(sessionId);

        Map<String, Object> session = new HashMap<>();
        session.put(FIELD_MEMBER_ID, String.valueOf(memberId));
        session.put(FIELD_ROLE, role);
        session.put(FIELD_IP_ADDRESS, maskIpAddress(ipAddress));
        session.put(FIELD_USER_AGENT, safeValue(userAgent));
        session.put(FIELD_LOGIN_AT, Instant.now().toString());
        session.put(FIELD_REFRESH_KEY, refreshKey);

        redisTemplate.opsForValue().set(refreshKey, sessionId, ttl);
        redisTemplate.opsForHash().putAll(sessionKey, session);
        redisTemplate.expire(sessionKey, ttl);
        redisTemplate.opsForSet().add(memberSessionKey(memberId), sessionId);
        redisTemplate.expire(memberSessionKey(memberId), ttl);

        if (isAdminRole(role)) {
            redisTemplate.opsForValue().set(adminSessionKey(memberId), sessionId, ttl);
        }

        return new LoginSession(sessionId, memberId, role);
    }

    public Optional<LoginSession> findSessionByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        Object sessionIdValue = redisTemplate.opsForValue().get(refreshKey(refreshToken));
        if (sessionIdValue == null) {
            return Optional.empty();
        }

        String sessionId = String.valueOf(sessionIdValue);
        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (session.isEmpty()) {
            redisTemplate.delete(refreshKey(refreshToken));
            return Optional.empty();
        }

        return Optional.of(toLoginSession(sessionId, session));
    }

    public Optional<Long> findMemberId(String refreshToken) {
        return findSessionByRefreshToken(refreshToken).map(LoginSession::getMemberId);
    }

    public Optional<AdminSessionInfoDto> findCurrentAdminSession(Long memberId) {
        Object sessionIdValue = redisTemplate.opsForValue().get(adminSessionKey(memberId));
        if (sessionIdValue == null) {
            return Optional.empty();
        }

        String sessionId = String.valueOf(sessionIdValue);
        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (session.isEmpty()) {
            redisTemplate.delete(adminSessionKey(memberId));
            return Optional.empty();
        }

        AdminSessionInfoDto info = new AdminSessionInfoDto();
        info.setLoginAt(toStringValue(session.get(FIELD_LOGIN_AT)));
        info.setIpAddress(toStringValue(session.get(FIELD_IP_ADDRESS)));
        info.setDevice(toDeviceName(toStringValue(session.get(FIELD_USER_AGENT))));
        return Optional.of(info);
    }

    public boolean isCurrentAdminSession(Long memberId, String sessionId) {
        if (memberId == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }

        Object currentSessionId = redisTemplate.opsForValue().get(adminSessionKey(memberId));
        return sessionId.equals(String.valueOf(currentSessionId));
    }

    public boolean sessionExists(Long memberId, String sessionId) {
        if (memberId == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }

        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (session.isEmpty()) {
            redisTemplate.opsForSet().remove(memberSessionKey(memberId), sessionId);
            return false;
        }

        String storedMemberId = toStringValue(session.get(FIELD_MEMBER_ID));
        return String.valueOf(memberId).equals(storedMemberId);
    }

    public void rotate(String oldRefreshToken, String newRefreshToken, Duration ttl) {
        LoginSession session = findSessionByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> new IllegalStateException("Refresh session was not found."));

        String oldRefreshKey = refreshKey(oldRefreshToken);
        String newRefreshKey = refreshKey(newRefreshToken);
        String sessionKey = sessionKey(session.getSessionId());

        redisTemplate.delete(oldRefreshKey);
        redisTemplate.opsForValue().set(newRefreshKey, session.getSessionId(), ttl);
        redisTemplate.opsForHash().put(sessionKey, FIELD_REFRESH_KEY, newRefreshKey);
        redisTemplate.expire(sessionKey, ttl);
        redisTemplate.expire(memberSessionKey(session.getMemberId()), ttl);

        if (isAdminRole(session.getRole())) {
            redisTemplate.opsForValue().set(
                    adminSessionKey(session.getMemberId()),
                    session.getSessionId(),
                    ttl
            );
        }
    }

    public void delete(String refreshToken) {
        findSessionByRefreshToken(refreshToken).ifPresent(this::deleteSession);
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(refreshKey(refreshToken));
        }
    }

    public void deleteCurrentAdminSession(Long memberId) {
        Object sessionIdValue = redisTemplate.opsForValue().get(adminSessionKey(memberId));
        if (sessionIdValue == null) {
            return;
        }

        String sessionId = String.valueOf(sessionIdValue);
        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        deleteSession(sessionId, session);
    }

    public int deleteMemberSessions(Long memberId) {
        if (memberId == null) {
            return 0;
        }

        Set<String> sessionIds = new HashSet<>();
        Set<Object> indexedSessionIds = redisTemplate.opsForSet().members(memberSessionKey(memberId));
        if (indexedSessionIds != null) {
            indexedSessionIds.forEach(value -> sessionIds.add(String.valueOf(value)));
        }

        Set<String> sessionKeys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        if (sessionKeys != null) {
            for (String key : sessionKeys) {
                Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
                if (String.valueOf(memberId).equals(toStringValue(session.get(FIELD_MEMBER_ID)))) {
                    sessionIds.add(key.substring(SESSION_KEY_PREFIX.length()));
                }
            }
        }

        int deletedCount = 0;
        for (String sessionId : sessionIds) {
            Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(sessionId));
            if (!session.isEmpty()) {
                deletedCount++;
            }
            deleteSession(sessionId, session);
        }

        redisTemplate.delete(memberSessionKey(memberId));
        return deletedCount;
    }

    private void deleteSession(LoginSession session) {
        Map<Object, Object> stored = redisTemplate.opsForHash().entries(sessionKey(session.getSessionId()));
        deleteSession(session.getSessionId(), stored);
    }

    private void deleteSession(String sessionId, Map<Object, Object> session) {
        String refreshKey = toStringValue(session.get(FIELD_REFRESH_KEY));
        String memberId = toStringValue(session.get(FIELD_MEMBER_ID));
        String role = toStringValue(session.get(FIELD_ROLE));

        if (refreshKey != null) {
            redisTemplate.delete(refreshKey);
        }

        redisTemplate.delete(sessionKey(sessionId));

        if (memberId != null) {
            redisTemplate.opsForSet().remove(memberSessionKey(Long.valueOf(memberId)), sessionId);
        }

        if (memberId != null && isAdminRole(role)) {
            String adminSessionKey = adminSessionKey(Long.valueOf(memberId));
            Object currentSessionId = redisTemplate.opsForValue().get(adminSessionKey);
            if (sessionId.equals(String.valueOf(currentSessionId))) {
                redisTemplate.delete(adminSessionKey);
            }
        }
    }

    private LoginSession toLoginSession(String sessionId, Map<Object, Object> session) {
        Long memberId = Long.valueOf(toStringValue(session.get(FIELD_MEMBER_ID)));
        String role = toStringValue(session.get(FIELD_ROLE));
        return new LoginSession(sessionId, memberId, role);
    }

    private String refreshKey(String refreshToken) {
        return REFRESH_KEY_PREFIX + sha256(refreshToken);
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String adminSessionKey(Long memberId) {
        return ADMIN_SESSION_KEY_PREFIX + memberId;
    }

    private String memberSessionKey(Long memberId) {
        return MEMBER_SESSION_KEY_PREFIX + memberId;
    }

    private boolean isAdminRole(String role) {
        return "ADM".equals(role) || "SUA".equals(role);
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }

        int lastDot = ipAddress.lastIndexOf('.');
        if (lastDot > 0) {
            return ipAddress.substring(0, lastDot + 1) + "*";
        }

        return ipAddress;
    }

    private String toDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }

        String browser = userAgent.contains("Edg/")
                ? "Edge"
                : userAgent.contains("Chrome/")
                ? "Chrome"
                : userAgent.contains("Firefox/")
                ? "Firefox"
                : userAgent.contains("Safari/")
                ? "Safari"
                : "Browser";
        String os = userAgent.contains("Windows")
                ? "Windows"
                : userAgent.contains("Mac OS X")
                ? "macOS"
                : userAgent.contains("Android")
                ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad")
                ? "iOS"
                : "Device";

        return browser + " / " + os;
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    @Getter
    public static class LoginSession {

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

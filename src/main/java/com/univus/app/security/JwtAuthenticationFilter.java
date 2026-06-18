package com.univus.app.security;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.member.service.RefreshTokenRedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberMapper memberMapper;
    private final AuthCookieUtil authCookieUtil;
    private final RefreshTokenRedisService refreshTokenRedisService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long memberId = jwtTokenProvider.getMemberId(token);
            String role = jwtTokenProvider.getRole(token);
            String sessionId = jwtTokenProvider.getSessionId(token);
            MemberDto member = memberMapper.findByMemberId(memberId);

            if (member != null
                    && !"WITHDRAWN".equals(member.getStatus())
                    && role != null
                    && role.equals(member.getRole())
                    && isSessionAllowed(memberId, role, sessionId)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                memberId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        return authCookieUtil
                .getCookieValue(request, AuthCookieUtil.ACCESS_TOKEN_COOKIE)
                .or(() -> authCookieUtil.getCookieValue(
                        request,
                        AuthCookieUtil.WEBSOCKET_ACCESS_TOKEN_COOKIE))
                .orElse(null);
    }

    private boolean isSessionAllowed(Long memberId, String role, String sessionId) {
        if (!"ADM".equals(role) && !"SUA".equals(role)) {
            return true;
        }

        return refreshTokenRedisService.isCurrentAdminSession(memberId, sessionId);
    }
}

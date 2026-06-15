package com.univus.app.security;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
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

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberMapper memberMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 요청 헤더에서 Bearer access token을 추출한다.
        String token = resolveToken(request);

        // 토큰이 있고 유효하면 JWT에서 사용자 식별값과 권한을 꺼내 인증 객체를 만든다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long memberId = jwtTokenProvider.getMemberId(token);
            String role = jwtTokenProvider.getRole(token);
            MemberDto member = memberMapper.findByMemberId(memberId);

            if (member != null
                    && "ACTIVE".equals(member.getStatus())
                    && role != null
                    && role.equals(member.getRole())) {
                // Spring Security는 SecurityContext에 인증 객체가 있어야 인증된 요청으로 판단한다.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                memberId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 인증 여부와 관계없이 다음 필터로 요청을 넘긴다.
        // 보호 API라면 이후 Security 설정에서 인증 여부를 검사한다.
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // Authorization: Bearer {token} 형식일 때만 실제 토큰 문자열을 반환한다.
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

}

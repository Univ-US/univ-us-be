package com.univus.app.config;

import com.univus.app.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
			// REST API 서버이므로 CSRF 토큰 검증을 비활성화한다.
			// JWT 기반 인증에서는 세션 쿠키를 사용하지 않기 때문에 CSRF 보호 대상이 아니다.
            .csrf(csrf -> csrf.disable())

			// 폼 로그인 화면과 브라우저 기본 인증을 사용하지 않는다.
			// 로그인은 /api/auth/login에서 JSON 요청으로 처리한다.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

			// JWT 기반 인증은 서버 세션을 만들지 않는 stateless 방식으로 동작한다.
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			.exceptionHandling(exception -> exception
					.authenticationEntryPoint((request, response, authException) -> {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType("application/json;charset=UTF-8");
						response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\"}");
					})
			)

			// 인증 없이 접근 가능한 공개 API를 지정하고,
			// 그 외 요청은 인증된 사용자만 접근하도록 설정한다.
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers(
            	        "/",
//            	        "/api/test/**",
//            	        "/api/auth/**",
						"/api/auth/signup",
						"/api/auth/login",
						"/api/auth/refresh",
						"/api/auth/logout",
            	        "/oauth2/**",
            	        "/ws-univus/**",


						//ai챗봇
						"/api/ai/**",

            	        // 커뮤니티 게시판
            	        "/api/posts/**",

            	        // 중고거래 상품
            	        "/api/products/**",

            	        // 거래 내역 + 채팅
            	        "/api/trades/**",
            	        "/api/chat/**",

            	        // 결제
            	        "/api/payments/**",

						// LMS
						//TODO: ERD ROLE 코드(STU/PROF/ADM/SUA/ALU)
						"/api/lms/professor/**",
						"/api/lms/student/**"

            	    ).permitAll()
            	    .anyRequest().authenticated()
            )
				// JWT 토큰 인증 필터 등록
				// JWT 인증 필터를 Spring Security 기본 로그인 필터보다 먼저 실행한다.
				// Authorization 헤더의 Bearer 토큰을 검증하고 SecurityContext에 인증 정보를 저장하기 위해 필요하다.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
            // TODO:  oauth2Login 추가예정

        return http.build();
    }
}	
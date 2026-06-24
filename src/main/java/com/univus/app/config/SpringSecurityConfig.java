package com.univus.app.config;

import com.univus.app.commoncode.code.RoleCode;
import com.univus.app.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
@Configuration

@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("UNIVUS-CSRF-TOKEN");
        csrfTokenRepository.setCookiePath("/api");

        http
            .cors(Customizer.withDefaults())

            .headers(headers -> headers
                    .referrerPolicy(referrerPolicy -> referrerPolicy
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(permissionsPolicy -> permissionsPolicy
                            .policy("camera=(), microphone=()"))
            )

			// 인증 JWT가 HttpOnly 쿠키로 전송되므로 상태 변경 요청은 CSRF 토큰도 검증한다.
			// PortOne webhook은 브라우저 요청이 아니므로 다음 서명 검증 작업으로 보호한다.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers("/api/subscriptions/webhooks/portone")
            )

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
					.accessDeniedHandler((request, response, accessDeniedException) -> {
						boolean csrfFailure = accessDeniedException instanceof CsrfException;
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						response.setContentType("application/json;charset=UTF-8");
						response.getWriter().write(
								csrfFailure
										? "{\"success\":false,\"code\":\"CSRF_INVALID\",\"message\":\"CSRF 토큰이 유효하지 않습니다.\"}"
										: "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\"}"
						);
					})
			)

			// 인증 없이 접근 가능한 공개 API를 지정하고,
			// 그 외 요청은 인증된 사용자만 접근하도록 설정한다.
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/admin/universities/*").hasAnyRole("ADM", "SUA")
                    .requestMatchers(HttpMethod.POST, "/api/admin/members/bulk").hasAnyRole("ADM", "SUA")
                    .requestMatchers(HttpMethod.POST, "/api/admin/support").permitAll()
					.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(
                            "/api/posts/**",
                            "/api/market/**",
                            "/api/trades/**",
                            "/api/chat/**"
                    ).hasAnyRole("SUA", "ADM", "STU", "ALU")
            	    .requestMatchers(
											"/",
//            	       "/api/test/**",
//            	       "/api/auth/**",
							"/api/auth/signup",
							"/api/auth/check-member-id",
							"/api/auth/check-login-id",
							"/api/auth/csrf",
							"/api/auth/login",
							"/api/auth/refresh",
							"/api/auth/logout",
							"/api/auth/admin/login",
							"/api/auth/user/login",
							"/api/auth/recovery/**",
            	        "/oauth2/**",
            	        "/ws-univus/**",

						// 날씨
						"/api/weather",

						// 대학 목록 (비로그인 가능)
						"/api/admin/universities",

						// 공통코드 (FE 드롭다운, 비로그인 가능)
						"/api/common-codes/**",

            	        // 게시글 이미지
            	        "/uploads/community/post/**",
            	        "/uploads/community/market/**",

            	        // 결제
            	        "/api/payments/**",

						// 구독 플랜 조회
						"/api/subscriptions/plans",

            	        // 레디스
            	        "/api/redis",
            	        "/api/redis/*",
            	        // 예약
                    "/api/reservations/date-options",
            	        "/api/reservations/seats/availability",
            	        "/api/reservations/seats/availability/**",

						// 프로필 이미지(정적 파일) — img 태그가 토큰을 못 실어 보내므로 공개
						"/uploads/lms/professor/image/**",
						"/uploads/lms/student/image/**"
            	    ).permitAll()
					.requestMatchers("/api/service-admin/reservations/penalties/**")
							.hasAnyRole(RoleCode.SUA.getCode(), RoleCode.ADM.getCode())
					.requestMatchers("/api/service-admin/community/**")
							.hasAnyRole(RoleCode.SUA.getCode(), RoleCode.ADM.getCode())
					.requestMatchers("/api/service-admin/**").hasRole("SUA")
							.requestMatchers(HttpMethod.POST,  "/api/subscriptions/webhooks/portone").permitAll()
							.requestMatchers("/api/lms/professor/**").hasAnyRole(RoleCode.PROF.getCode()) // 교수 lms는 교수만 (교수 및 강의 배정 등의 관리는 BO-학교 관리자측)
							.requestMatchers("/api/lms/student/**").hasAnyRole(RoleCode.STU.getCode(), RoleCode.ALU.getCode()) // 학생 lms는 재학생과 졸업생만(학생 관리는 BO-학교 관리자측)
            	    .anyRequest().authenticated()
            )
				// JWT 토큰 인증 필터 등록
				// JWT 인증 필터를 Spring Security 기본 로그인 필터보다 먼저 실행한다.
				// Authorization 헤더의 Bearer 토큰을 검증하고 SecurityContext에 인증 정보를 저장하기 위해 필요하다.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

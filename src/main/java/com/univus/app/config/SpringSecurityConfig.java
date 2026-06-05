package com.univus.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers(
            	        "/",
            	        "/api/test/**",
            	        "/api/auth/**",
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
            	        "/api/payments/**"

            	    ).permitAll()
            	    .anyRequest().authenticated()
            );
            
            // TODO:  oauth2Login 설정과 JWT 필터(addFilterBefore)가 추가예정

        return http.build();
    }
}	
package com.univus.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // React에서 웹소켓에 접속할 엔드포인트 (예: ws://localhost:8080/ws-univus)
        registry.addEndpoint("/ws-univus")
                .setAllowedOrigins(
                        "http://localhost:3000",        // 로컬 개발
                        "http://192.168.0.108",         // LAN 배포(현 HTTP) — 배포 환경 WS 핸드셰이크 403 수정
                        "https://www.univus.com",       // TLS-A(self-signed, hosts 가상도메인) 대비
                        "https://univus.com",           // TLS-A 대비
                        "https://happyjob.iptime.org"   // TLS-B(Let's Encrypt) 대비
                ) // React 도메인 허용
                .withSockJS(); // 브라우저 호환성을 위한 SockJS 사용
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 받을 때 사용할 프리픽스
        registry.enableSimpleBroker("/sub");
        
        // 클라이언트가 서버로 메시지를 보낼 때 사용할 프리픽스
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
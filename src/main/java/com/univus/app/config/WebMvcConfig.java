package com.univus.app.config;

import com.univus.app.security.SubscriptionAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SubscriptionAccessInterceptor subscriptionAccessInterceptor;

    @Value("${file.upload-root}")
    private String uploadRoot;

    // React와의 통신을 위한 CORS 설정 
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://www.localhost:3000",
                        "http://localhost:9090",
                        "http://192.168.0.108",
                        "http://192.168.0.108:3000",
                        "http://192.168.0.108:9090",
                        "http://192.168.0.115:9090",
                        "http://192.168.0.115:3000",
                        "https://www.univus.com",       // TLS-A(self-signed) 대비
                        "https://univus.com",           // TLS-A 대비
                        "https://univus.duckdns.org",   // TLS prod(Let's Encrypt, LE) - 외부/webhook 도메인
                        "https://happyjob.iptime.org"   // TLS-B(Let's Encrypt) 대비
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // 파일 업로드 리소스 핸들러 필요시 경로 수정 . 
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadRoot + "/");
    }

    // 관리자, LMS, 커뮤니티 API 요청 전에 대학 구독 상태에 따른 접근 가능 여부를 검사합니다.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subscriptionAccessInterceptor)
                .addPathPatterns(
                        "/api/admin/**",
                        "/api/lms/**",
                        "/api/posts/**",
                        "/api/market/**",
                        "/api/trades/**",
                        "/api/chat/**"
                )
                .excludePathPatterns(
                        "/api/admin/support",
                        "/api/admin/universities"
                );
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

}

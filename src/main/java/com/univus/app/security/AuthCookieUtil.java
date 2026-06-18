package com.univus.app.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String WEBSOCKET_ACCESS_TOKEN_COOKIE = "wsAccessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final boolean secure;
    private final String sameSite;

    public AuthCookieUtil(
            @Value("${auth.cookie.secure:false}") boolean secure,
            @Value("${auth.cookie.same-site:Lax}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void addAccessTokenSessionCookie(HttpServletResponse response, String token) {
        addSessionCookie(response, ACCESS_TOKEN_COOKIE, token, "/api");
        addWebSocketAccessTokenSessionCookie(response, token);
    }

    public void addWebSocketAccessTokenSessionCookie(
            HttpServletResponse response,
            String token
    ) {
        addSessionCookie(
                response,
                WEBSOCKET_ACCESS_TOKEN_COOKIE,
                token,
                "/ws-univus"
        );
    }

    public void addRefreshTokenSessionCookie(HttpServletResponse response, String token) {
        addSessionCookie(response, REFRESH_TOKEN_COOKIE, token, "/api/auth");
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", "/api", Duration.ZERO);
        addCookie(
                response,
                WEBSOCKET_ACCESS_TOKEN_COOKIE,
                "",
                "/ws-univus",
                Duration.ZERO
        );
        addCookie(response, REFRESH_TOKEN_COOKIE, "", "/api/auth", Duration.ZERO);
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path,
            Duration maxAge
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void addSessionCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

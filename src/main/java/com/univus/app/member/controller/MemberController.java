package com.univus.app.member.controller;

import com.univus.app.member.dto.AuthSessionResponseDto;
import com.univus.app.member.dto.AdminSessionConflictResponseDto;
import com.univus.app.member.dto.LoginRequestDto;
import com.univus.app.member.dto.LoginResponseDto;
import com.univus.app.member.dto.RefreshTokenResponseDto;
import com.univus.app.member.dto.SignupRequestDto;
import com.univus.app.member.exception.AdminSessionConflictException;
import com.univus.app.member.service.MemberService;
import com.univus.app.security.AuthCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class MemberController {

  private final MemberService memberService;
  private final AuthCookieUtil authCookieUtil;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public void signup(@RequestBody SignupRequestDto request) {
    memberService.signup(request);
  }

  @PostMapping("/login")
  public LoginResponseDto login(
          @RequestBody LoginRequestDto request,
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse
  ) {
    LoginResponseDto response = memberService.login(
            request,
            httpServletRequest.getRemoteAddr(),
            httpServletRequest.getHeader("User-Agent")
    );
    addAuthCookies(httpServletResponse, response.getAccessToken(), response.getRefreshToken());
    return response;
  }

  @PostMapping("/admin/login")
  public LoginResponseDto adminLogin(
          @RequestBody LoginRequestDto request,
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse
  ) {
    LoginResponseDto response = memberService.adminLogin(
            request,
            httpServletRequest.getRemoteAddr(),
            httpServletRequest.getHeader("User-Agent")
    );
    addAuthCookies(httpServletResponse, response.getAccessToken(), response.getRefreshToken());
    return response;
  }

  @PostMapping("/user/login")
  public LoginResponseDto userLogin(
          @RequestBody LoginRequestDto request,
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse
  ) {
    LoginResponseDto response = memberService.userLogin(
            request,
            httpServletRequest.getRemoteAddr(),
            httpServletRequest.getHeader("User-Agent")
    );
    addAuthCookies(httpServletResponse, response.getAccessToken(), response.getRefreshToken());
    return response;
  }

  @GetMapping("/check-login-id")
  public Map<String, Boolean> checkLoginId(@RequestParam String loginId) {
    return Map.of("available", memberService.isLoginIdAvailable(loginId));
  }

  @PostMapping("/refresh")
  public RefreshTokenResponseDto refreshAccessToken(
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse
  ) {
    String refreshToken = authCookieUtil
            .getCookieValue(httpServletRequest, AuthCookieUtil.REFRESH_TOKEN_COOKIE)
            .orElse(null);
    RefreshTokenResponseDto response = memberService.refreshAccessToken(refreshToken);
    addAuthCookies(httpServletResponse, response.getAccessToken(), response.getRefreshToken());
    return response;
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse
  ) {
    authCookieUtil
            .getCookieValue(httpServletRequest, AuthCookieUtil.REFRESH_TOKEN_COOKIE)
            .ifPresent(memberService::logout);
    authCookieUtil.clearAuthCookies(httpServletResponse);
  }

  @GetMapping("/me")
  public AuthSessionResponseDto me(
          @AuthenticationPrincipal Long memberId,
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse
  ) {
    authCookieUtil
            .getCookieValue(
                    httpServletRequest,
                    AuthCookieUtil.ACCESS_TOKEN_COOKIE
            )
            .ifPresent(token ->
                    authCookieUtil.addWebSocketAccessTokenSessionCookie(
                            httpServletResponse,
                            token
                    ));
    return memberService.getSession(memberId);
  }

  private void addAuthCookies(
          HttpServletResponse response,
          String accessToken,
          String refreshToken
  ) {
    authCookieUtil.addAccessTokenSessionCookie(
            response,
            accessToken
    );
    authCookieUtil.addRefreshTokenSessionCookie(
            response,
            refreshToken
    );
  }

  @ExceptionHandler(AdminSessionConflictException.class)
  public ResponseEntity<AdminSessionConflictResponseDto> handleAdminSessionConflict(
          AdminSessionConflictException ex
  ) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getResponse());
  }
}

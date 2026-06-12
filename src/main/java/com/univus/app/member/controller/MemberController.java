package com.univus.app.member.controller;

import com.univus.app.member.dto.*;
import com.univus.app.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class MemberController {

  private final MemberService memberService;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public void signup(@RequestBody SignupRequestDto request) {
    memberService.signup(request);
  }

  @PostMapping("/login")
  public LoginResponseDto login(@RequestBody LoginRequestDto request,
                                HttpServletRequest httpServletRequest) {
    return memberService.login(request, httpServletRequest.getRemoteAddr());
  }

  // SUA, ADM, GUEST 로그인
  @PostMapping("/admin/login")
  public LoginResponseDto adminLogin(@RequestBody LoginRequestDto request,
                                     HttpServletRequest httpServletRequest) {
    return memberService.adminLogin(request, httpServletRequest.getRemoteAddr());
  }

  // PROF, STU, ALU 로그인
  @PostMapping("/user/login")
  public LoginResponseDto userLogin(@RequestBody LoginRequestDto request,
                                    HttpServletRequest httpServletRequest) {
    return memberService.userLogin(request, httpServletRequest.getRemoteAddr());
  }

  @GetMapping("/check-login-id")
  public Map<String, Boolean> checkLoginId(@RequestParam String loginId) {
    return Map.of("available", memberService.isLoginIdAvailable(loginId));
  }

  @PostMapping("/refresh")
  public RefreshTokenResponseDto refreshAccessToken(@RequestBody RefreshTokenRequestDto request) {
    return memberService.refreshAccessToken(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody LogoutRequestDto request) {
    memberService.logout(request);
  }

  // 인증 확인용 API - 테스트용
  @GetMapping("/me")
  public Long me(Authentication authentication) {
    return Long.valueOf(authentication.getPrincipal().toString());
  }
}

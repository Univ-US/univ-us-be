package com.univus.app.member.controller;

import com.univus.app.member.dto.LoginRequestDto;
import com.univus.app.member.dto.LoginResponseDto;
import com.univus.app.member.dto.LogoutRequestDto;
import com.univus.app.member.dto.SignupRequestDto;
import com.univus.app.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody LogoutRequestDto request) {
    memberService.logout(request);
  }

  // 인증 확인용 API - 테스트용
  @GetMapping("/me")
  public Long me(Authentication authentication) {
    return (Long) authentication.getPrincipal();
  }
}

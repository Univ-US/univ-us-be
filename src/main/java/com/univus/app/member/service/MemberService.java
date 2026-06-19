package com.univus.app.member.service;

import com.univus.app.member.dto.AuthSessionResponseDto;
import com.univus.app.member.dto.LoginRequestDto;
import com.univus.app.member.dto.LoginResponseDto;
import com.univus.app.member.dto.RefreshTokenResponseDto;
import com.univus.app.member.dto.SignupRequestDto;

public interface MemberService {

  void signup(SignupRequestDto request);

  LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent);

  LoginResponseDto adminLogin(LoginRequestDto request, String ipAddress, String userAgent);

  LoginResponseDto userLogin(LoginRequestDto request, String ipAddress, String userAgent);

  boolean isLoginIdAvailable(String loginId);

  void logout(String refreshToken);

  AuthSessionResponseDto getSession(Long memberId);

  RefreshTokenResponseDto refreshAccessToken(String refreshToken);

  RefreshTokenResponseDto createAdminSession(Long memberId, String ipAddress, String userAgent);
}

package com.univus.app.member.service;

import com.univus.app.member.dto.*;
import com.univus.app.member.exception.DuplicateMemberException;
import com.univus.app.member.exception.InvalidLoginException;
import com.univus.app.member.exception.InvalidLogoutException;
import com.univus.app.member.exception.InvalidRefreshTokenException;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class MemberService {

  private static final String DEFAULT_ROLE = "GUEST";
  private static final String DEFAULT_STATUS = "ACTIVE";

  private final MemberMapper memberMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public void signup(SignupRequestDto request) {

    if (memberMapper.existsByLoginId(request.getLoginId()) > 0) {
      throw new DuplicateMemberException("이미 사용 중인 아이디입니다.");
    }

    MemberDto member = new MemberDto();
    member.setLoginId(request.getLoginId());
    member.setPassword(passwordEncoder.encode(request.getPassword()));
    member.setMemberName(request.getMemberName());
    member.setRole(DEFAULT_ROLE);
    member.setPhoneNumber(request.getPhoneNumber());
    member.setGender(request.getGender());
    member.setStatus(DEFAULT_STATUS);
    member.setBirth(request.getBirth());

    int inserted = memberMapper.insertMember(member);

    if (inserted != 1) {
      throw new IllegalStateException("회원가입 처리에 실패했습니다.");
    }
  }

  // 응답용
  private MemberResponseDto toResponse(MemberDto member) {
    MemberResponseDto response = new MemberResponseDto();
    response.setMemberId(member.getMemberId());
    response.setUnivId(member.getUnivId());
    response.setDeptId(member.getDeptId());
    response.setMemberName(member.getMemberName());
    response.setRole(member.getRole());
    response.setPhoneNumber(member.getPhoneNumber());
    response.setGender(member.getGender());
    response.setStatus(member.getStatus());
    response.setCreatedAt(member.getCreatedAt());
    response.setLogtimeAt(member.getLogtimeAt());
    response.setBirth(member.getBirth());
    response.setCommunityNickname(member.getCommunityNickname());
    return response;
  }

  @Transactional(noRollbackFor = InvalidLoginException.class)
  public LoginResponseDto login(LoginRequestDto request, String ipAddress) {
    return loginWithAllowedRoles(request, ipAddress, null);
  }

  @Transactional(noRollbackFor = InvalidLoginException.class)
  public LoginResponseDto adminLogin(LoginRequestDto request, String ipAddress) {
    return loginWithAllowedRoles(request, ipAddress, Set.of("SUA", "ADM", "GUEST"));
  }

  @Transactional(noRollbackFor = InvalidLoginException.class)
  public LoginResponseDto userLogin(LoginRequestDto request, String ipAddress) {
    return loginWithAllowedRoles(request, ipAddress, Set.of("ADM", "PROF", "STU", "ALU"));
  }

  @Transactional(readOnly = true)
  public boolean isLoginIdAvailable(String loginId) {
    return loginId != null
            && !loginId.isBlank()
            && memberMapper.existsByLoginId(loginId) == 0;
  }

  private LoginResponseDto loginWithAllowedRoles(
      LoginRequestDto request,
      String ipAddress,
      Set<String> allowedRoles
  ) {
    MemberDto member = memberMapper.findByLoginIdAndUnivId(request.getLoginId(), request.getUnivId());

    if (member == null) {
      insertLoginFailLog(null, "MEMBER_NOT_FOUND");
      throw new InvalidLoginException("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
      insertLoginFailLog(member.getMemberId(), "INVALID_PASSWORD");
      throw new InvalidLoginException("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    if (allowedRoles != null && !allowedRoles.contains(member.getRole())) {
      insertLoginFailLog(member.getMemberId(), "ROLE_NOT_ALLOWED");
      throw new InvalidLoginException("허용되지 않은 로그인 유형입니다.");
    }

    String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId(), member.getRole());
    String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

    memberMapper.updateLogtimeAt(member.getMemberId());

    LoginLogDto loginLog = new LoginLogDto();
    loginLog.setMemberId(member.getMemberId());
    loginLog.setResult("SUCCESS");
    memberMapper.insertLoginLog(loginLog);

    LoginSessionDto loginSession = new LoginSessionDto();
    loginSession.setMemberId(member.getMemberId());
    loginSession.setRefreshToken(refreshToken);
    loginSession.setIpAddress(ipAddress);
    memberMapper.insertLoginSession(loginSession);

    LoginResponseDto response = new LoginResponseDto();
    response.setAccessToken(accessToken);
    response.setRefreshToken(refreshToken);
    response.setTokenType(jwtTokenProvider.getTokenType());
    response.setMemberId(member.getMemberId());
    response.setRole(member.getRole());
    response.setUnivId(member.getUnivId());
    response.setUnivName(member.getUnivName());
    response.setMemberName(member.getMemberName());
    response.setCommunityNickname(member.getCommunityNickname());

    return response;
  }

  @Transactional
  public void logout(LogoutRequestDto request) {

    LoginSessionDto loginSession =
            memberMapper.findActiveLoginSessionByRefreshToken(request.getRefreshToken());

    if (loginSession == null) {
      throw new InvalidLogoutException("유효하지 않은 로그아웃 요청입니다.");
    }

    int revoked = memberMapper.revokeLoginSession(request.getRefreshToken());

    if (revoked != 1) {
      throw new InvalidLogoutException("유효하지 않은 로그아웃 요청입니다.");
    }

    LoginLogDto loginLog = new LoginLogDto();
    loginLog.setMemberId(loginSession.getMemberId());
    loginLog.setResult("LOGOUT");
    memberMapper.insertLoginLog(loginLog);
  }

  private void insertLoginFailLog(Long memberId, String failReason) {
    LoginLogDto loginLog = new LoginLogDto();
    loginLog.setMemberId(memberId);
    loginLog.setResult("FAIL");
    loginLog.setFailReason(failReason);
    memberMapper.insertLoginLog(loginLog);
  }

  @Transactional(readOnly = true)
  public RefreshTokenResponseDto refreshAccessToken(RefreshTokenRequestDto request) {

    if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
      throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
    }

    if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
      throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
    }

    LoginSessionDto loginSession =
            memberMapper.findActiveLoginSessionByRefreshToken(request.getRefreshToken());

    if (loginSession == null) {
      throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
    }

    MemberDto member = memberMapper.findByMemberId(loginSession.getMemberId());

    if (member == null) {
      throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
    }

    String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId(), member.getRole());

    RefreshTokenResponseDto response = new RefreshTokenResponseDto();
    response.setAccessToken(accessToken);
    response.setTokenType(jwtTokenProvider.getTokenType());
    response.setMemberId(member.getMemberId());
    response.setRole(member.getRole());

    return response;
  }

}

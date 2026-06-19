package com.univus.app.member.service;

import com.univus.app.member.dto.AuthSessionResponseDto;
import com.univus.app.member.dto.LoginLogDto;
import com.univus.app.member.dto.LoginRequestDto;
import com.univus.app.member.dto.LoginResponseDto;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.dto.RefreshTokenResponseDto;
import com.univus.app.member.dto.SignupRequestDto;
import com.univus.app.member.exception.AdminSessionConflictException;
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
public class MemberServiceImpl implements MemberService {

  private static final String DEFAULT_ROLE = "GUEST";
  private static final String DEFAULT_STATUS = "ACTIVE";
  private static final String STATUS_WITHDRAWN = "WITHDRAWN";
  private static final String ROLE_ADMIN = "ADM";
  private static final String ROLE_SUPER_ADMIN = "SUA";

  private final MemberMapper memberMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRedisService refreshTokenRedisService;

  @Transactional
  @Override
  public void signup(SignupRequestDto request) {
    if (memberMapper.existsByLoginId(request.getLoginId()) > 0) {
      throw new DuplicateMemberException("Login id is already in use.");
    }

    MemberDto member = new MemberDto();
    member.setLoginId(request.getLoginId());
    member.setPassword(passwordEncoder.encode(request.getPassword()));
    member.setMemberName(request.getMemberName());
    member.setDeptId(request.getDeptId());
    member.setCommunityNickname(normalizeNickname(request.getCommunityNickname()));
    member.setRole(DEFAULT_ROLE);
    member.setPhoneNumber(request.getPhoneNumber());
    member.setGender(request.getGender());
    member.setStatus(DEFAULT_STATUS);
    member.setBirth(request.getBirth());

    int inserted = memberMapper.insertMember(member);

    if (inserted != 1) {
      throw new IllegalStateException("Failed to create member.");
    }

    if (member.getDeptId() != null) {
      MemberDto createdMember = memberMapper.findByLoginId(request.getLoginId());
      if (createdMember == null || createdMember.getMemberId() == null) {
        throw new IllegalStateException("Failed to load created member.");
      }

      member.setMemberId(createdMember.getMemberId());
      int detailInserted = memberMapper.insertMemberDetail(member);
      if (detailInserted != 1) {
        throw new IllegalStateException("Failed to create member detail.");
      }
    }
  }

  @Transactional(noRollbackFor = {InvalidLoginException.class, AdminSessionConflictException.class})
  @Override
  public LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent) {
    MemberDto member = memberMapper.findByLoginId(request.getLoginId());
    return loginWithAllowedRoles(member, request, ipAddress, userAgent, null);
  }

  @Transactional(noRollbackFor = {InvalidLoginException.class, AdminSessionConflictException.class})
  @Override
  public LoginResponseDto adminLogin(LoginRequestDto request, String ipAddress, String userAgent) {
    MemberDto member = memberMapper.findByLoginId(request.getLoginId());
    return loginWithAllowedRoles(member, request, ipAddress, userAgent, Set.of("SUA", "ADM", "GUEST"));
  }

  @Transactional(noRollbackFor = {InvalidLoginException.class, AdminSessionConflictException.class})
  @Override
  public LoginResponseDto userLogin(LoginRequestDto request, String ipAddress, String userAgent) {
    MemberDto member = memberMapper.findByLoginIdAndUnivId(
            request.getLoginId(),
            request.getUnivId()
    );
    return loginWithAllowedRoles(member, request, ipAddress, userAgent, Set.of("ADM", "PROF", "STU", "ALU"));
  }

  @Transactional(readOnly = true)
  @Override
  public boolean isLoginIdAvailable(String loginId) {
    return loginId != null
            && !loginId.isBlank()
            && memberMapper.existsByLoginId(loginId) == 0;
  }

  private LoginResponseDto loginWithAllowedRoles(
          MemberDto member,
          LoginRequestDto request,
          String ipAddress,
          String userAgent,
          Set<String> allowedRoles
  ) {
    if (member == null) {
      insertLoginFailLog(null, "MEMBER_NOT_FOUND");
      throw new InvalidLoginException("Invalid login id or password.");
    }

    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
      insertLoginFailLog(member.getMemberId(), "INVALID_PASSWORD");
      throw new InvalidLoginException("Invalid login id or password.");
    }

    if (allowedRoles != null && !allowedRoles.contains(member.getRole())) {
      insertLoginFailLog(member.getMemberId(), "ROLE_NOT_ALLOWED");
      throw new InvalidLoginException("Login type is not allowed for this role.");
    }

    if (STATUS_WITHDRAWN.equals(member.getStatus())) {
      insertLoginFailLog(member.getMemberId(), "ACCOUNT_NOT_ACTIVE");
      throw new InvalidLoginException("Account is not active.");
    }

    if (isAdminRole(member.getRole())) {
      if (Boolean.TRUE.equals(request.getForceLogin())) {
        refreshTokenRedisService.deleteCurrentAdminSession(member.getMemberId());
      } else {
        refreshTokenRedisService.findCurrentAdminSession(member.getMemberId())
                .ifPresent(session -> {
                  insertLoginFailLog(member.getMemberId(), "CONCURRENT_ADMIN_SESSION");
                  throw new AdminSessionConflictException(session);
                });
      }
    }

    RefreshTokenResponseDto tokens = createSessionTokens(
            member.getMemberId(),
            member.getRole(),
            ipAddress,
            userAgent
    );

    memberMapper.updateLogtimeAt(member.getMemberId());

    LoginLogDto loginLog = new LoginLogDto();
    loginLog.setMemberId(member.getMemberId());
    loginLog.setResult("SUCCESS");
    memberMapper.insertLoginLog(loginLog);

    LoginResponseDto response = toLoginResponse(member);
    response.setAccessToken(tokens.getAccessToken());
    response.setRefreshToken(tokens.getRefreshToken());
    response.setTokenType(tokens.getTokenType());
    return response;
  }

  @Transactional
  @Override
  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new InvalidLogoutException("Invalid logout request.");
    }

    Long memberId = refreshTokenRedisService.findMemberId(refreshToken)
            .orElseThrow(() -> new InvalidLogoutException("Invalid logout request."));

    refreshTokenRedisService.delete(refreshToken);

    LoginLogDto loginLog = new LoginLogDto();
    loginLog.setMemberId(memberId);
    loginLog.setResult("LOGOUT");
    memberMapper.insertLoginLog(loginLog);
  }

  @Transactional(readOnly = true)
  @Override
  public AuthSessionResponseDto getSession(Long memberId) {
    MemberDto member = memberMapper.findByMemberId(memberId);

    if (member == null || STATUS_WITHDRAWN.equals(member.getStatus())) {
      throw new InvalidRefreshTokenException("Invalid authentication session.");
    }

    AuthSessionResponseDto response = new AuthSessionResponseDto();
    response.setMemberId(member.getMemberId());
    response.setRole(member.getRole());
    response.setUnivId(member.getUnivId());
    response.setUnivName(member.getUnivName());
    response.setMemberName(member.getMemberName());
    response.setCommunityNickname(member.getCommunityNickname());
    response.setStatus(member.getStatus());
    response.setPhoneNumber(member.getPhoneNumber());
    response.setCreatedAt(member.getCreatedAt());
    return response;
  }

  @Transactional
  @Override
  public RefreshTokenResponseDto refreshAccessToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new InvalidRefreshTokenException("Invalid refresh token.");
    }

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      refreshTokenRedisService.delete(refreshToken);
      throw new InvalidRefreshTokenException("Invalid refresh token.");
    }

    RefreshTokenRedisService.LoginSession session =
            refreshTokenRedisService.findSessionByRefreshToken(refreshToken)
                    .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token."));

    MemberDto member = memberMapper.findByMemberId(session.getMemberId());

    if (member == null) {
      refreshTokenRedisService.delete(refreshToken);
      throw new InvalidRefreshTokenException("Invalid refresh token.");
    }

    if (STATUS_WITHDRAWN.equals(member.getStatus())) {
      refreshTokenRedisService.delete(refreshToken);
      throw new InvalidRefreshTokenException("Inactive account.");
    }

    if (isAdminRole(member.getRole())
            && !refreshTokenRedisService.isCurrentAdminSession(
            member.getMemberId(),
            session.getSessionId()
    )) {
      refreshTokenRedisService.delete(refreshToken);
      throw new InvalidRefreshTokenException("The administrator session has expired.");
    }

    String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());
    refreshTokenRedisService.rotate(
            refreshToken,
            newRefreshToken,
            jwtTokenProvider.getRefreshTokenValidity()
    );

    RefreshTokenResponseDto response = new RefreshTokenResponseDto();
    response.setAccessToken(jwtTokenProvider.createAccessToken(
            member.getMemberId(),
            member.getRole(),
            session.getSessionId()
    ));
    response.setRefreshToken(newRefreshToken);
    response.setTokenType(jwtTokenProvider.getTokenType());
    response.setMemberId(member.getMemberId());
    response.setRole(member.getRole());

    return response;
  }

  @Transactional
  @Override
  public RefreshTokenResponseDto createAdminSession(Long memberId, String ipAddress, String userAgent) {
    MemberDto member = memberMapper.findByMemberId(memberId);

    if (member == null || STATUS_WITHDRAWN.equals(member.getStatus())) {
      throw new InvalidRefreshTokenException("Invalid authentication session.");
    }

    refreshTokenRedisService.deleteCurrentAdminSession(memberId);
    return createSessionTokens(memberId, ROLE_ADMIN, ipAddress, userAgent);
  }

  private RefreshTokenResponseDto createSessionTokens(
          Long memberId,
          String role,
          String ipAddress,
          String userAgent
  ) {
    String refreshToken = jwtTokenProvider.createRefreshToken(memberId);
    RefreshTokenRedisService.LoginSession session =
            refreshTokenRedisService.createSession(
                    refreshToken,
                    memberId,
                    role,
                    ipAddress,
                    userAgent,
                    jwtTokenProvider.getRefreshTokenValidity()
            );

    RefreshTokenResponseDto response = new RefreshTokenResponseDto();
    response.setAccessToken(jwtTokenProvider.createAccessToken(memberId, role, session.getSessionId()));
    response.setRefreshToken(refreshToken);
    response.setTokenType(jwtTokenProvider.getTokenType());
    response.setMemberId(memberId);
    response.setRole(role);
    return response;
  }

  private LoginResponseDto toLoginResponse(MemberDto member) {
    LoginResponseDto response = new LoginResponseDto();
    response.setMemberId(member.getMemberId());
    response.setRole(member.getRole());
    response.setUnivId(member.getUnivId());
    response.setUnivName(member.getUnivName());
    response.setMemberName(member.getMemberName());
    response.setCommunityNickname(member.getCommunityNickname());
    response.setStatus(member.getStatus());
    response.setPhoneNumber(member.getPhoneNumber());
    response.setCreatedAt(member.getCreatedAt());
    return response;
  }

  private void insertLoginFailLog(Long memberId, String failReason) {
    LoginLogDto loginLog = new LoginLogDto();
    loginLog.setMemberId(memberId);
    loginLog.setResult("FAIL");
    loginLog.setFailReason(failReason);
    memberMapper.insertLoginLog(loginLog);
  }

  private boolean isAdminRole(String role) {
    return ROLE_ADMIN.equals(role) || ROLE_SUPER_ADMIN.equals(role);
  }

  private String normalizeNickname(String communityNickname) {
    if (communityNickname == null) {
      return null;
    }

    String trimmed = communityNickname.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
